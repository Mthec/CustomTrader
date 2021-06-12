package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;

@SuppressWarnings("ConstantConditions")
public abstract class OtherTraderTradingWindow<T extends Trade> extends TradingWindow {
    protected static final Logger logger = Logger.getLogger(CurrencyTradingWindow.class.getName());
    protected static final Map<String, Logger> loggers = new HashMap<>();
    protected final Creature windowOwner;
    protected final Creature watcher;
    protected final boolean offer;
    protected final long wurmId;
    protected Set<Item> items;
    protected final T trade;

    protected OtherTraderTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, T trade) {
        this.windowOwner = owner;
        this.watcher = watcher;
        this.offer = offer;
        this.wurmId = wurmId;
        this.trade = trade;
    }

    public static void stopLoggers() {
        Iterator<Logger> var0 = loggers.values().iterator();

        while(true) {
            Logger logger;
            do {
                if (!var0.hasNext()) {
                    return;
                }

                logger = var0.next();
            } while(logger == null);

            for (Handler h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static Logger getLogger(long wurmId) {
        String name = "othertrader" + wurmId;
        Logger personalLogger = loggers.get(name);
        if (personalLogger == null) {
            personalLogger = Logger.getLogger(name);
            personalLogger.setUseParentHandlers(false);
            Handler[] h = logger.getHandlers();

            for(int i = 0; i != h.length; ++i) {
                personalLogger.removeHandler(h[i]);
            }

            try {
                FileHandler fh = new FileHandler(name + ".log", 0, 1, true);
                fh.setFormatter(new SimpleFormatter());
                personalLogger.addHandler(fh);
            } catch (IOException var6) {
                Logger.getLogger(name).log(Level.WARNING, name + ":no redirection possible!");
            }

            loggers.put(name, personalLogger);
        }

        return personalLogger;
    }

    @Override
    public boolean mayMoveItemToWindow(Item item, Creature creature, long window) {
        boolean toReturn = false;
        if (this.wurmId == 3L) {
            if (window == 1L) {
                toReturn = true;
            }
        } else if (this.wurmId == 4L) {
            if (window == 2L) {
                toReturn = true;
            }
        } else if (this.wurmId == 2L) {
            if (!this.windowOwner.equals(creature)) {
                if (creature.isPlayer() && item.isCoin() && !this.windowOwner.isPlayer()) {
                    return false;
                }

                if (window == 4L) {
                    toReturn = true;
                }
            }
        } else if (this.wurmId == 1L && !this.windowOwner.equals(creature) && window == 3L && this.watcher == creature && (item.getOwnerId() == this.windowOwner.getWurmId() || item.isCoin())) {
            toReturn = true;
        }

        return toReturn;
    }

    @Override
    public boolean mayAddFromInventory(Creature creature, Item item) {
        if (!item.isTraded()) {
            if (item.isNoTrade()) {
                creature.getCommunicator().sendSafeServerMessage(item.getNameWithGenus() + " is not tradable.");
            } else if (this.windowOwner.equals(creature)) {
                try {
                    long owner = item.getOwner();
                    if (owner != this.watcher.getWurmId() && owner != this.windowOwner.getWurmId()) {
                        this.windowOwner.setCheated("Traded " + item.getName() + "[" + item.getWurmId() + "] with " + this.watcher.getName() + " owner=" + owner);
                    }
                } catch (NotOwnedException var8) {
                    this.windowOwner.setCheated("Traded " + item.getName() + "[" + item.getWurmId() + "] with " + this.watcher.getName() + " not owned?");
                }

                if (this.wurmId == 2L || this.wurmId == 1L) {
                    if (item.isHollow()) {
                        Item[] its = item.getAllItems(true);

                        for (Item lIt : its) {
                            if (lIt.isNoTrade() || lIt.isVillageDeed() || lIt.isHomesteadDeed() || lIt.getTemplateId() == 781) {
                                creature.getCommunicator().sendSafeServerMessage(item.getNameWithGenus() + " contains a non-tradable item.");
                                return false;
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public long getWurmId() {
        return wurmId;
    }

    @Override
    public Item[] getItems() {
        return this.items != null ? this.items.toArray(new Item[0]) : new Item[0];
    }

    private void removeExistingContainedItems(Item item) {
        if (item.isHollow()) {
            for (Item lElement : item.getItemsAsArray()) {
                this.removeExistingContainedItems(lElement);
                if (lElement.getTradeWindow() == this) {
                    this.removeFromTrade(lElement, false);
                } else if (lElement.getTradeWindow() != null) {
                    lElement.getTradeWindow().removeItem(lElement);
                }
            }
        }

    }

    @Override
    public Item[] getAllItems() {
        if (this.items == null) {
            return new Item[0];
        } else {
            Set<Item> toRet = new HashSet<>();

            for (Item item : this.items) {
                toRet.add(item);
                Item[] toAdd = item.getAllItems(false);

                for (Item lElement : toAdd) {
                    if (lElement.tradeWindow == this) {
                        toRet.add(lElement);
                    }
                }
            }

            return toRet.toArray(new Item[0]);
        }
    }

    @Override
    public void stopReceivingItems() {
    }

    @Override
    public void startReceivingItems() {
    }

    @Override
    public void addItem(Item item) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }

        if (item.tradeWindow == null) {
            this.removeExistingContainedItems(item);
            Item parent = item;

            try {
                parent = item.getParent();
            } catch (NoSuchItemException ignored) {}

            this.items.add(item);
            this.addToTrade(item);
            if (item == parent || parent.isViewableBy(this.windowOwner)) {
                if (!this.windowOwner.isPlayer()) {
                    this.windowOwner.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, 0);
                } else if (!this.watcher.isPlayer()) {
                    this.windowOwner.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, this.watcher.getTradeHandler().getTraderBuyPriceForItem(item));
                } else {
                    this.windowOwner.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, item.getPrice());
                }
            }

            if (item == parent || parent.isViewableBy(this.watcher)) {
                if (!this.watcher.isPlayer()) {
                    this.watcher.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, 0);
                } else if (!this.windowOwner.isPlayer()) {
                    this.watcher.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, this.windowOwner.getTradeHandler().getTraderSellPriceForItem(item, this));
                } else {
                    this.watcher.getCommunicator().sendAddToInventory(item, this.wurmId, parent.tradeWindow == this ? parent.getWurmId() : 0L, item.getPrice());
                }
            }
        }

        this.tradeChanged();
    }

    private void addToTrade(Item item) {
        if (item.tradeWindow != this) {
            item.setTradeWindow(this);
        }

        for (Item lIt : item.getItems()) {
            addToTrade(lIt);
        }
    }

    private void removeFromTrade(Item item, boolean noSwap) {
        this.windowOwner.getCommunicator().sendRemoveFromInventory(item, this.wurmId);
        this.watcher.getCommunicator().sendRemoveFromInventory(item, this.wurmId);
        if (noSwap && item.isCoin()) {
            if (item.getOwnerId() == -10L) {
                Economy.getEconomy().returnCoin(item, "Notrade", true);
            }
        }
        item.setTradeWindow(null);

    }

    @Override
    public void removeItem(Item item) {
        if (this.items != null && item.tradeWindow == this) {
            this.removeExistingContainedItems(item);
            this.items.remove(item);
            this.removeFromTrade(item, true);
            this.tradeChanged();
        }
    }

    @Override
    public void updateItem(Item item) {
        if (this.items != null && item.tradeWindow == this) {
            if (!this.windowOwner.isPlayer()) {
                this.windowOwner.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, 0);
            } else if (!this.watcher.isPlayer()) {
                this.windowOwner.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, this.watcher.getTradeHandler().getTraderBuyPriceForItem(item));
            } else {
                this.windowOwner.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, item.getPrice());
            }

            if (!this.watcher.isPlayer()) {
                this.watcher.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, 0);
            } else if (!this.windowOwner.isPlayer()) {
                this.watcher.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, this.windowOwner.getTradeHandler().getTraderSellPriceForItem(item, this));
            } else {
                this.watcher.getCommunicator().sendUpdateInventoryItem(item, this.wurmId, item.getPrice());
            }

            this.tradeChanged();
        }
    }

    private void tradeChanged() {
        if (this.wurmId == 2L && !this.trade.creatureTwo.isPlayer()) {
            this.trade.setCreatureTwoSatisfied(false);
        }

        if (this.wurmId == 3L || this.wurmId == 4L) {
            this.trade.setCreatureOneSatisfied(false);
            this.trade.setCreatureTwoSatisfied(false);
            int c = this.trade.getNextTradeId();
            this.windowOwner.getCommunicator().sendTradeChanged(c);
            this.watcher.getCommunicator().sendTradeChanged(c);
        }
    }

    @Override
    boolean hasInventorySpace() {
        if (this.offer) {
            this.windowOwner.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            this.watcher.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            logger.log(Level.WARNING, "Inconsistency! This is offer window number " + this.wurmId + ". Traders are " + this.watcher.getName() + ", " + this.windowOwner.getName());
            return false;
        } else if (!(this.watcher instanceof Player)) {
            return true;
        } else {
            Item inventory = this.watcher.getInventory();
            if (inventory == null) {
                this.windowOwner.getCommunicator().sendAlertServerMessage("Could not find inventory for " + this.watcher.getName() + ". Trade aborted.");
                this.watcher.getCommunicator().sendAlertServerMessage("Could not find your inventory item. Trade aborted. Please contact administrators.");
                logger.log(Level.WARNING, "Failed to locate inventory for " + this.watcher.getName());
                return false;
            } else {
                if (this.items != null) {
                    int nums = 0;

                    for (Item item : this.items) {
                        if (!inventory.testInsertItem(item)) {
                            return false;
                        }

                        if (!item.isCoin()) {
                            ++nums;
                        }

                        if (!item.canBeDropped(false) && this.watcher.isGuest()) {
                            this.windowOwner.getCommunicator().sendAlertServerMessage("Guests cannot receive the item " + item.getName() + ".");
                            this.watcher.getCommunicator().sendAlertServerMessage("Guests cannot receive the item " + item.getName() + ".");
                            return false;
                        }
                    }

                    if (this.watcher.getPower() <= 0 && nums + inventory.getNumItemsNotCoins() > 99) {
                        this.watcher.getCommunicator().sendAlertServerMessage("You may not carry that many items in your inventory.");
                        this.windowOwner.getCommunicator().sendAlertServerMessage(this.watcher.getName() + " may not carry that many items in " + this.watcher.getHisHerItsString() + " inventory.");
                        return false;
                    }
                }

                return true;
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    int getWeight() {
        int toReturn = 0;
        Item item;
        if (this.items != null) {
            for(Iterator<Item> var2 = this.items.iterator(); var2.hasNext(); toReturn += item.getFullWeight()) {
                item = var2.next();
            }
        }

        return toReturn;
    }

    protected boolean itemNotValid(Item item) {
        if ((this.windowOwner instanceof Player || !item.isCoin()) && item.getOwnerId() != this.windowOwner.getWurmId()) {
            this.windowOwner.getCommunicator().sendAlertServerMessage(item.getName() + " is not owned by you. Trade aborted.");
            this.watcher.getCommunicator().sendAlertServerMessage(item.getName() + " is not owned by " + this.windowOwner.getName() + ". Trade aborted.");
            return true;
        }

        return false;
    }

    @Override
    boolean validateTrade() {
        if (this.windowOwner.isDead()) {
            return false;
        } else if (this.windowOwner instanceof Player && !this.windowOwner.hasLink()) {
            return false;
        } else {
            if (this.items != null) {

                for (Item tit : this.items) {
                    if (itemNotValid(tit)) {
                        return false;
                    }

                    Item[] allItems = tit.getAllItems(false);

                    for (Item lAllItem : allItems) {
                        if (itemNotValid(lAllItem)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    protected abstract int handleItemTransfer(Item item);

    @Override
    void swapOwners() {
        if (!offer) {
            Item inventory = watcher.getInventory();
            int tradeValueCounter = 0;
            Shop shop;
            if (CustomTraderMod.isOtherTrader(windowOwner)) {
                shop = Economy.getEconomy().getShop(this.windowOwner);
            } else {
                shop = Economy.getEconomy().getShop(this.watcher);
            }

            if (items != null) {
                for (Item item : items) {
                    removeExistingContainedItems(item);
                    removeFromTrade(item, false);

                    if (!(this.windowOwner instanceof Player)) {
                        if (this.watcher.isLogged()) {
                            this.watcher.getLogger().log(Level.INFO, this.windowOwner.getName() + " trading " + item.getName() + " with id " + item.getWurmId() + " from " + this.watcher.getName());
                        }
                    } else if (!(this.watcher instanceof Player)) {
                        if (this.windowOwner.isLogged()) {
                            this.windowOwner.getLogger().log(Level.INFO, this.windowOwner.getName() + " selling " + item.getName() + " with id " + item.getWurmId() + " to " + this.watcher.getName());
                        }
                    }

                    try {
                        Item parent = Items.getItem(item.getParentId());
                        parent.dropItem(item.getWurmId(), false);
                    } catch (NoSuchItemException var36) {
                        logger.log(Level.WARNING, "Parent not found for item " + item.getWurmId());
                    }

                    getLogger(shop.getWurmId()).log(Level.INFO, this.watcher.getName() + " received " + MaterialUtilities.getMaterialString(item.getMaterial()) + " " + item.getName() + ", id: " + item.getWurmId() + ", QL: " + item.getQualityLevel());

                    tradeValueCounter += handleItemTransfer(item);
                }
            }

            this.windowOwner.getCommunicator().sendNormalServerMessage("The trade was completed successfully.");

            if (CustomTraderMod.isOtherTrader(watcher)) {
                shop.addMoneyEarned(tradeValueCounter);
            }
        } else {
            this.windowOwner.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            this.watcher.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            logger.log(Level.WARNING, "Inconsistency! This is offer window number " + this.wurmId + ". Traders are " + this.watcher.getName() + ", " + this.windowOwner.getName());
        }
    }

    @Override
    void endTrade() {
        if (this.items != null) {
            for (Item item : items.toArray(new Item[0])) {
                this.removeExistingContainedItems(item);
                this.items.remove(item);
                this.removeFromTrade(item, true);
            }
        }

        this.items = null;
    }
}
