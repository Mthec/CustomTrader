package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class OtherTraderTrade<V> extends Trade {
    protected static final Logger logger = Logger.getLogger(OtherTraderTrade.class.getName());
    protected final TradingWindow creatureOneOfferWindow;
    protected final TradingWindow creatureTwoOfferWindow;
    protected final TradingWindow creatureOneRequestWindow;
    protected final TradingWindow creatureTwoRequestWindow;
    protected boolean creatureOneSatisfied = false;
    protected boolean creatureTwoSatisfied = false;
    protected int currentCounter = -1;

    protected OtherTraderTrade(Creature player, Creature trader, Supplier<V> value) {
        creatureOne = player;
        creatureOne.startTrading();
        creatureTwo = trader;
        creatureTwo.startTrading();
        creatureTwoOfferWindow = createTradingWindow(trader, player, true, 1L, value);
        creatureOneOfferWindow = createTradingWindow(player, trader, true, 2L, value);
        creatureOneRequestWindow = createTradingWindow(trader, player, false, 3L, value);
        creatureTwoRequestWindow = createTradingWindow(player, trader, false, 4L, value);
    }

    protected abstract TradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, Supplier<V> value);

    @Override
    public void addShopDiff(long money) {}

    @Override
    public TradingWindow getTradingWindow(long id) {
        switch ((int)id) {
            case 1:
                return creatureTwoOfferWindow;
            case 2:
                return creatureOneOfferWindow;
            case 3:
                return creatureOneRequestWindow;
            case 4:
            default:
                return creatureTwoRequestWindow;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void setSatisfied(Creature creature, boolean satisfied, int id) {
        if (id == currentCounter) {
            if (creature.equals(creatureOne)) {
                creatureOneSatisfied = satisfied;
            } else {
                creatureTwoSatisfied = satisfied;
            }

            if (creatureOneSatisfied && creatureTwoSatisfied) {
                if (makeTrade()) {
                    creatureOne.getCommunicator().sendCloseTradeWindow();
                    creatureTwo.getCommunicator().sendCloseTradeWindow();
                } else {
                    creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                    creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
                }
            } else {
                creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
            }
        }
    }

    @Override
    int getNextTradeId() {
        return ++currentCounter;
    }

    @SuppressWarnings("DuplicatedCode")
    private boolean makeTrade() {
        if ((!creatureOne.isPlayer() || creatureOne.hasLink()) && !creatureOne.isDead()) {
            if ((!creatureTwo.isPlayer() || creatureTwo.hasLink()) && !creatureTwo.isDead()) {
                if (creatureOneRequestWindow.hasInventorySpace() && creatureTwoRequestWindow.hasInventorySpace()) {
                    int reqOneWeight = creatureOneRequestWindow.getWeight();
                    int reqTwoWeight = creatureTwoRequestWindow.getWeight();
                    int diff = reqOneWeight - reqTwoWeight;
                    if (diff > 0 && creatureOne instanceof Player && !creatureOne.canCarry(diff)) {
                        creatureTwo.getCommunicator().sendNormalServerMessage(creatureOne.getName() + " cannot carry that much.", (byte)3);
                        creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        if (creatureOne.getPower() > 0) {
                            creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much. You would carry " + diff + " more.");
                        }

                        return false;
                    }

                    diff = reqTwoWeight - reqOneWeight;
                    if (diff > 0 && creatureTwo instanceof Player && !creatureTwo.canCarry(diff)) {
                        creatureOne.getCommunicator().sendNormalServerMessage(creatureTwo.getName() + " cannot carry that much.", (byte)3);
                        creatureTwo.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        return false;
                    }

                    boolean ok = creatureOneRequestWindow.validateTrade();
                    if (!ok) {
                        return false;
                    }

                    ok = creatureTwoRequestWindow.validateTrade();
                    if (ok) {
                        creatureOneRequestWindow.swapOwners();
                        creatureTwoRequestWindow.swapOwners();
                        creatureTwoOfferWindow.endTrade();
                        creatureOneOfferWindow.endTrade();

                        if (CustomTraderMod.isOtherTrader(creatureTwo)) {
                            Shop shop = Economy.getEconomy().getShop(creatureTwo);
                            shop.setMerchantData(creatureTwo.getNumberOfShopItems());
                            CustomTraderDatabase.restock(creatureTwo);
                        }

                        creatureOne.setTrade(null);
                        creatureTwo.setTrade(null);
                        return true;
                    }
                }

                return false;
            } else {
                if (creatureTwo.hasLink()) {
                    creatureTwo.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
                }

                creatureOne.getCommunicator().sendNormalServerMessage(creatureTwo.getName() + " cannot trade right now.", (byte)3);
                end(creatureTwo, false);
                return true;
            }
        } else {
            if (creatureOne.hasLink()) {
                creatureOne.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
            }

            creatureTwo.getCommunicator().sendNormalServerMessage(creatureOne.getName() + " cannot trade right now.", (byte)3);
            end(creatureOne, false);
            return true;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void end(Creature creature, boolean closed) {
        try {
            if (creature.equals(creatureOne)) {
                creatureTwo.getCommunicator().sendCloseTradeWindow();
                if (!closed) {
                    creatureOne.getCommunicator().sendCloseTradeWindow();
                }

                creatureTwo.getCommunicator().sendNormalServerMessage(creatureOne.getName() + " withdrew from the trade.", (byte)2);
                creatureOne.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
            } else {
                creatureOne.getCommunicator().sendCloseTradeWindow();
                if (!closed || !creatureTwo.isPlayer()) {
                    creatureTwo.getCommunicator().sendCloseTradeWindow();
                }

                creatureOne.getCommunicator().sendNormalServerMessage(creatureTwo.getName() + " withdrew from the trade.", (byte)2);
                creatureTwo.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
            }
        } catch (Exception var4) {
            logger.log(Level.WARNING, var4.getMessage(), var4);
        }

        creatureTwoOfferWindow.endTrade();
        creatureOneOfferWindow.endTrade();
        creatureOneRequestWindow.endTrade();
        creatureTwoRequestWindow.endTrade();
        creatureOne.setTrade(null);
        creatureTwo.setTrade(null);
    }

    @Override
    boolean isCreatureOneSatisfied() {
        return creatureOneSatisfied;
    }

    @Override
    void setCreatureOneSatisfied(boolean aCreatureOneSatisfied) {
        creatureOneSatisfied = aCreatureOneSatisfied;
    }

    @Override
    boolean isCreatureTwoSatisfied() {
        return creatureTwoSatisfied;
    }

    @Override
    void setCreatureTwoSatisfied(boolean aCreatureTwoSatisfied) {
        creatureTwoSatisfied = aCreatureTwoSatisfied;
    }

    @Override
    public int getCurrentCounter() {
        return currentCounter;
    }

    @Override
    void setCurrentCounter(int aCurrentCounter) {
        currentCounter = aCurrentCounter;
    }

    @Override
    public long getTax() {
        throw new UnsupportedOperationException("Method not used");
    }

    @Override
    public void setTax(long aTax) {
        throw new UnsupportedOperationException("Method not used");
    }

    @Override
    public TradingWindow getCreatureOneRequestWindow() {
        return creatureOneRequestWindow;
    }

    @Override
    public TradingWindow getCreatureTwoRequestWindow() {
        return creatureTwoRequestWindow;
    }

    @Override
    Creature getCreatureOne() {
        return creatureOne;
    }

    @Override
    Creature getCreatureTwo() {
        return creatureTwo;
    }
}
