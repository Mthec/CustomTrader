package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Items;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CustomTraderTradeHandler;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.*;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.*;
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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
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
        TradeSetup.preInit();
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

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseCreatureCreationQuestion",
                "(Lcom/wurmonline/server/questions/CreatureCreationQuestion;)V",
                () -> this::creatureCreation);


        TradeSetup.init(manager);
        FaceSetter.init(manager);
        ModelSetter.init(manager, new CustomTraderWearItems());
        DestroyHandler.addListener(creature -> CustomTraderDatabase.deleteTrader((Creature)creature));
        ModCreatures.init();
        ModCreatures.addCreature(new CustomTraderTemplate());
        ModCreatures.addCreature(new CurrencyTraderTemplate());
        ModCreatures.addCreature(new StatTraderTemplate());
    }

    @Override
    public void onServerStarted() {
        TradeSetup.addTrader(this::isSpecialTrader, CustomTraderTradeHandler::create);
        faceSetter = new FaceSetter(this::isSpecialTrader, dbName);
        modelSetter = new ModelSetter(this::isSpecialTrader, dbName);

        ModActions.registerAction(new OtherTraderTradeAction());
        ModActions.registerAction(new ManageCustomTraderAction());
        new PlaceCustomTraderAction();
        new PlaceCurrencyTraderAction();
        new PlaceStatTraderAction();
        PlaceNpcMenu.register();
        CustomiserPlayerGiveAction.register(this::isSpecialTrader, new CanGiveRemoveGMAndWearable() {
            @Override
            public boolean canGive(@NotNull Creature performer, @NotNull Item item, @NotNull Creature target) {
                boolean other = super.canGive(performer, item, target);
                if (other && Arrays.stream(CustomTraderDatabase.getStockFor(target)).anyMatch(it -> it.item.matches(item))) {
                    performer.getCommunicator().sendNormalServerMessage(target.getName() + " cannot accept the " + item.getName() + ", as they would mix it up with their stock.");
                    return false;
                }
                return other;
            }
        });
        CustomiserPlayerRemoveAction.register(this::isSpecialTrader, new CanGiveRemoveGMAndWearable());

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
