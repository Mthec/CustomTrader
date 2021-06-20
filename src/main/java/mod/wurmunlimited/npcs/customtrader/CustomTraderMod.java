package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Items;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.*;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import javassist.*;
import mod.wurmunlimited.npcs.DestroyHandler;
import mod.wurmunlimited.npcs.FaceSetter;
import mod.wurmunlimited.npcs.ModelSetter;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Favor;
import mod.wurmunlimited.npcs.customtrader.stats.FavorPriest;
import mod.wurmunlimited.npcs.customtrader.stats.Health;
import mod.wurmunlimited.npcs.customtrader.stats.Karma;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class CustomTraderMod implements WurmServerMod, Configurable, PreInitable, Initable, ServerStartedListener, PlayerMessageListener {
    private static final Logger logger = Logger.getLogger(CustomTraderMod.class.getName());
    public static final String dbName = "customtrader.db";
    public static final int maxTagLength = 25;
    public static final int maxNameLength = 20;
    public static CustomTraderMod mod;
    public static String namePrefix = "Trader";
    public FaceSetter faceSetter;
    public ModelSetter modelSetter;
    private boolean preventDecay = true;
    private final CommandWaitTimer restockTimer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);

    public CustomTraderMod() {
        mod = this;
    }

    static {
        Karma.register();
        Health.register();
        Favor.register();
        FavorPriest.register();
    }

    public static boolean isOtherTrader(Creature maybeTrader) {
        return CurrencyTraderTemplate.isCurrencyTrader(maybeTrader) || StatTraderTemplate.is(maybeTrader);
    }

    @Override
    public void configure(Properties properties) {
        String val = properties.getProperty("prevent_decay", "true");
        preventDecay = val != null && val.equals("true");
        namePrefix =  properties.getProperty("name_prefix", "Trader");
    }

    @Override
    public void preInit() {
        ClassPool pool = HookManager.getInstance().getClassPool();

        try {
            // Remove final and add empty constructor to TradeHandler.
            CtClass tradeHandler = pool.get("com.wurmonline.server.creatures.TradeHandler");
            tradeHandler.defrost();
            tradeHandler.setModifiers(Modifier.clear(tradeHandler.getModifiers(), Modifier.FINAL));
            if (tradeHandler.getConstructors().length == 1)
                tradeHandler.addConstructor(CtNewConstructor.make(tradeHandler.getSimpleName() + "(){}", tradeHandler));

            // Remove final and add empty constructor to TradingWindow.
            CtClass tradingWindow = pool.get("com.wurmonline.server.items.TradingWindow");
            tradingWindow.defrost();
            tradingWindow.setModifiers(Modifier.clear(tradingWindow.getModifiers(), Modifier.FINAL));
            if (tradingWindow.getConstructors().length == 1)
                tradingWindow.addConstructor(CtNewConstructor.make(tradingWindow.getSimpleName() + "(){}", tradingWindow));

            // Remove final and add empty constructor to Trade.
            CtClass trade = pool.get("com.wurmonline.server.items.Trade");
            trade.defrost();
            if (trade.getConstructors().length == 1)
                trade.addConstructor(CtNewConstructor.make(trade.getSimpleName() + "(){}", trade));
            // Remove final from public fields.
            CtField creatureOne = trade.getDeclaredField("creatureOne");
            creatureOne.setModifiers(Modifier.clear(creatureOne.getModifiers(), Modifier.FINAL));
            CtField creatureTwo = trade.getDeclaredField("creatureTwo");
            creatureTwo.setModifiers(Modifier.clear(creatureTwo.getModifiers(), Modifier.FINAL));
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

        manager.registerHook("com.wurmonline.server.items.Item",
                "pollOwned",
                "(Lcom/wurmonline/server/creatures/Creature;)Z",
                () -> this::pollOwned);

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

        manager.registerHook("com.wurmonline.server.economy.Economy",
                "getShop",
                "(Lcom/wurmonline/server/creatures/Creature;Z)Lcom/wurmonline/server/economy/Shop;",
                () -> this::getShop);


        FaceSetter.init(manager);
        ModelSetter.init(manager);
        DestroyHandler.addListener(creature -> CustomTraderDatabase.deleteTrader((Creature)creature));
        ModCreatures.init();
        ModCreatures.addCreature(new CustomTraderTemplate());
        ModCreatures.addCreature(new CurrencyTraderTemplate());
        ModCreatures.addCreature(new StatTraderTemplate());
    }

    @Override
    public void onServerStarted() {
        faceSetter = new FaceSetter(this::isSpecialTrader, dbName);
        modelSetter = new ModelSetter(this::isSpecialTrader, dbName);

        ModActions.registerAction(new OtherTraderTradeAction());
        ModActions.registerAction(new ManageCustomTraderAction());
        new PlaceCustomTraderAction();
        new PlaceCurrencyTraderAction();
        new PlaceStatTraderAction();
        PlaceNpcMenu.register();

        try {
            CustomTraderDatabase.loadTags();
        } catch (SQLException e) {
            logger.warning("An error occurred when loading tags from dump.");
            e.printStackTrace();
        }
    }

    Object poll(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)o;
        boolean toDestroy = (boolean)method.invoke(o, args);

        if (!toDestroy && isSpecialTrader(creature)) {
            CustomTraderDatabase.restock(creature);
        }

        //noinspection SuspiciousInvocationHandlerImplementation
        return toDestroy;
    }

    Object pollOwned(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)args[0];
        Item item = (Item)o;
        if (preventDecay && isSpecialTrader(creature)) {
            item.setLastMaintained(WurmCalendar.currentTime);
            for (Item it : item.getAllItems(true)) {
                it.setLastMaintained(WurmCalendar.currentTime);
            }

            return false;
        } else {
            return method.invoke(o, args);
        }
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

        //noinspection SuspiciousInvocationHandlerImplementation
        return tradeCompleted;
    }

    Object getShop(Object o, Method method, Object[] args) throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Creature creature = (Creature)args[0];
        boolean destroying = (boolean)args[1];
        Shop tm;
        if (isOtherTrader(creature)) {
            ReentrantReadWriteLock SHOPS_RW_LOCK = ReflectionUtil.getPrivateField(null, Economy.class.getDeclaredField("SHOPS_RW_LOCK"));
            SHOPS_RW_LOCK.readLock().lock();

            try {
                Map<Long, Shop> shops = ReflectionUtil.getPrivateField(Economy.getEconomy(), Economy.class.getDeclaredField("shops"));
                tm = shops.get(creature.getWurmId());
            } finally {
                SHOPS_RW_LOCK.readLock().unlock();
            }

            if (!destroying && tm == null) {
                tm = Economy.getEconomy().createShop(creature.getWurmId());
            }

            return tm;
        } else {
            return method.invoke(o, args);
        }
    }

    Object getTradeHandler(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException, InstantiationException {
        Creature creature = (Creature)o;
        if (!isSpecialTrader(creature))
            return method.invoke(o, args);
        Field tradeHandler = Creature.class.getDeclaredField("tradeHandler");
        tradeHandler.setAccessible(true);
        TradeHandler handler = (TradeHandler) tradeHandler.get(creature);

        if (handler == null) {
            Class<?> ServiceHandler;

            if (CustomTraderTemplate.isCustomTrader(creature))
                ServiceHandler = Class.forName("com.wurmonline.server.creatures.CustomTraderTradeHandler");
            else if (CurrencyTraderTemplate.isCurrencyTrader(creature))
                ServiceHandler = Class.forName("com.wurmonline.server.creatures.CurrencyTraderTradeHandler");
            else
                ServiceHandler = Class.forName("com.wurmonline.server.creatures.StatTraderTradeHandler");
            handler = (TradeHandler)ServiceHandler.getConstructor(Creature.class, Trade.class).newInstance(creature, creature.getTrade());
            tradeHandler.set(o, handler);
        }

        return handler;
    }

    Object creatureCreation(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CreatureCreationQuestion question = (CreatureCreationQuestion)args[0];
        Properties answers = ReflectionUtil.getPrivateField(question, Question.class.getDeclaredField("answer"));
        try {
            String templateIndexString = answers.getProperty("data1");
            String name = answers.getProperty("cname");
            if (name == null)
                answers.setProperty("name", "");
            else
                answers.setProperty("name", name);
            if (templateIndexString != null) {
                int templateIndex = Integer.parseInt(templateIndexString);
                List<CreatureTemplate> templates = ReflectionUtil.getPrivateField(question, CreatureCreationQuestion.class.getDeclaredField("cretemplates"));
                CreatureTemplate template = templates.get(templateIndex);

                Creature responder = question.getResponder();
                int floorLevel = responder.getFloorLevel();
                VolaTile tile = Zones.getOrCreateTile(question.getTileX(), question.getTileY(), responder.isOnSurface());
                if (CustomTraderTemplate.isCustomTrader(template)) {
                    new PlaceCustomTraderQuestion(responder, tile, floorLevel).answer(answers);
                    return null;
                } else if (CurrencyTraderTemplate.isCurrencyTrader(template)) {
                    new PlaceCurrencyTraderQuestion(responder, tile, floorLevel).answer(answers);
                    return null;
                } else if (StatTraderTemplate.is(template)) {
                    new PlaceStatTraderQuestion(responder, tile, floorLevel).answer(answers);
                    return null;
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            question.getResponder().getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
            e.printStackTrace();
        }

        return method.invoke(o, args);
    }

    boolean isSpecialTrader(Creature trader) {
        return CustomTraderTemplate.isCustomTrader(trader) || isOtherTrader(trader);
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        Player player = communicator.getPlayer();

        if (player != null && player.getPower() >= 2) {
            if (message.equals("/dumptags")) {
                try {
                    CustomTraderDatabase.dumpTags();
                    communicator.sendSafeServerMessage("Custom Trader tags were successfully dumped.");
                } catch (SQLException e) {
                    communicator.sendAlertServerMessage("An error occurred when dumping tags.");
                    e.printStackTrace();
                }

                return MessagePolicy.DISCARD;
            } else if (message.startsWith("/restock")) {
                String timeRemaining = restockTimer.timeRemaining();
                if (timeRemaining.isEmpty()) {
                    String tag = message.replace("/restock", "").trim();

                    switch (CustomTraderDatabase.fullyStock(tag)) {
                        case SUCCESS:
                            player.getCommunicator().sendNormalServerMessage("All " + tag + " traders were restocked.");
                            break;
                        case NO_TRADERS_FOUND:
                            player.getCommunicator().sendNormalServerMessage("No traders were found with that tag.");
                            listTraderTags(player);
                            break;
                        case NO_TAG_RECEIVED:
                            player.getCommunicator().sendSafeServerMessage("You need to provide a tag to restock.");
                            listTraderTags(player);
                            break;
                        case NO_STOCK_FOR_TAG:
                            player.getCommunicator().sendNormalServerMessage("No stock was found for that tag.");
                            break;
                    }

                    restockTimer.reset();
                } else {
                    player.getCommunicator().sendNormalServerMessage("You need to wait " + timeRemaining + " before /restock can be used again.");
                }

                return MessagePolicy.DISCARD;
            } else if (message.startsWith("/tradertags")) {
                listTraderTags(player);

                return MessagePolicy.DISCARD;
            }
        }

        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    static void listTraderTags(Player player) {
        List<String> tags = CustomTraderDatabase.getAllTags();
        if (tags.isEmpty()) {
            player.getCommunicator().sendNormalServerMessage("There are no tags set for custom traders.");
        } else {
            tags.sort(String.CASE_INSENSITIVE_ORDER);
            player.getCommunicator().sendNormalServerMessage("The following custom trader tags are available - " +
                                                                     String.join(", ", tags) +
                                                                     ".");
        }
    }
}
