package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.ManageCustomTraderAction;
import com.wurmonline.server.behaviours.PlaceCustomTraderAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.PlaceCustomTraderQuestion;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import javassist.*;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CustomTraderMod implements WurmServerMod, PreInitable, Initable, ServerStartedListener {
    public static final int maxTagLength = 25;
    public static String namePrefix = "Trader";

    @Override
    public void preInit() {
        ClassPool pool = HookManager.getInstance().getClassPool();

        try {
            // Remove final from TradeHandler.
            CtClass tradeHandler = pool.get("com.wurmonline.server.creatures.TradeHandler");
            tradeHandler.defrost();
            tradeHandler.setModifiers(Modifier.clear(tradeHandler.getModifiers(), Modifier.FINAL));
            // Add empty constructor.
            if (tradeHandler.getConstructors().length == 1)
                tradeHandler.addConstructor(CtNewConstructor.make(tradeHandler.getSimpleName() + "(){}", tradeHandler));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "poll",
                "()Z",
                () -> this::poll);

        manager.registerHook("com.wurmonline.server.items.Trade",
                "makeTrade",
                "()Z",
                () -> this::makeTrade);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "getTradeHandler",
                "()Lcom/wurmonline/server/creatures/TradeHandler;",
                () -> this::getTradeHandler);

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseCreatureCreationQuestion",
                "(Lcom/wurmonline/server/questions/CreatureCreationQuestion;)V",
                () -> this::creatureCreation);

        ModCreatures.init();
        ModCreatures.addCreature(new CustomTraderTemplate());
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new ManageCustomTraderAction());
        ModActions.registerAction(new PlaceCustomTraderAction());
    }

    Object poll(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)o;
        boolean toDestroy = (boolean)method.invoke(o, args);

        if (!toDestroy && CustomTraderTemplate.isCustomTrader(creature)) {
            CustomTraderDatabase.restock(creature);
        }

        return toDestroy;
    }

    Object makeTrade(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = ((Trade)o).creatureTwo;
        Set<Item> startingItems = new HashSet<>(creature.getInventory().getItems());
        boolean tradeCompleted = (boolean)method.invoke(o, args);

        if (tradeCompleted && CustomTraderTemplate.isCustomTrader(creature)) {
            // To stop TradingWindow replacing any bought items.
            for (Item item : creature.getInventory().getItemsAsArray()) {
                if (!startingItems.contains(item)) {
                    Items.destroyItem(item.getWurmId());
                }
            }
            CustomTraderDatabase.restock(creature);
            creature.getShop().setMoney(0);
        }

        return tradeCompleted;
    }

    Object getTradeHandler(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException, InstantiationException {
        Creature creature = (Creature)o;
        if (!CustomTraderTemplate.isCustomTrader(creature))
            return method.invoke(o, args);
        Field tradeHandler = Creature.class.getDeclaredField("tradeHandler");
        tradeHandler.setAccessible(true);
        TradeHandler handler = (TradeHandler) tradeHandler.get(creature);

        if (handler == null) {
            Class<?> ServiceHandler = Class.forName("com.wurmonline.server.creatures.CustomTraderTradeHandler");
            handler = (TradeHandler)ServiceHandler.getConstructor(Creature.class, Trade.class).newInstance(creature, creature.getTrade());
            tradeHandler.set(o, handler);
        }

        return handler;
    }

    // TODO - Test.
    Object creatureCreation(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CreatureCreationQuestion question = (CreatureCreationQuestion)o;
        Properties answers = ReflectionUtil.getPrivateField(question, Question.class.getDeclaredField("answer"));
        try {
            int templateIndex = Integer.parseInt(answers.getProperty("data1"));
            List<CreatureTemplate> templates = ReflectionUtil.getPrivateField(question, CreatureCreationQuestion.class.getDeclaredField("cretemplates"));
            CreatureTemplate template = templates.get(templateIndex);

            if (CustomTraderTemplate.isCustomTrader(template)) {
                Creature responder = question.getResponder();
                int floorLevel = responder.getFloorLevel();
                VolaTile tile = Zones.getTileOrNull(question.getTileX(), question.getTileY(), responder.isOnSurface());
                Properties properties = new Properties();
                properties.setProperty("gender", (new Random().nextBoolean()) ? "male" : "female");
                new PlaceCustomTraderQuestion(responder, tile, floorLevel).answer(properties);
                return null;
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            question.getResponder().getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
            e.printStackTrace();
        }

        return method.invoke(o, args);
    }
}
