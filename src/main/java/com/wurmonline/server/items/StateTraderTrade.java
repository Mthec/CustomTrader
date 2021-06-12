package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.StatTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StateTraderTrade extends Trade {
    private static final Logger logger = Logger.getLogger(StateTraderTrade.class.getName());
    private final TradingWindow creatureOneOfferWindow;
    private final TradingWindow creatureTwoOfferWindow;
    private final TradingWindow creatureOneRequestWindow;
    private final TradingWindow creatureTwoRequestWindow;
    private boolean creatureOneSatisfied = false;
    private boolean creatureTwoSatisfied = false;
    private int currentCounter = -1;
    final Stat stat;

    public StateTraderTrade(Creature player, Creature trader, Stat stat) {
        creatureOne = player;
        creatureOne.startTrading();
        creatureTwo = trader;
        this.stat = stat;
        creatureTwo.startTrading();
        creatureTwoOfferWindow = new StatTradingWindow(trader, player, true, 1L, this);
        creatureOneOfferWindow = new StatTradingWindow(player, trader, true, 2L, this);
        creatureOneRequestWindow = new StatTradingWindow(trader, player, false, 3L, this);
        creatureTwoRequestWindow = new StatTradingWindow(player, trader, false, 4L, this);
    }

    @Override
    public void addShopDiff(long money) {
    }

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

    @Override
    public void setSatisfied(Creature creature, boolean satisfied, int id) {
        if (id == this.currentCounter) {
            if (creature.equals(this.creatureOne)) {
                this.creatureOneSatisfied = satisfied;
            } else {
                this.creatureTwoSatisfied = satisfied;
            }

            if (this.creatureOneSatisfied && this.creatureTwoSatisfied) {
                if (this.makeTrade()) {
                    this.creatureOne.getCommunicator().sendCloseTradeWindow();
                    this.creatureTwo.getCommunicator().sendCloseTradeWindow();
                } else {
                    this.creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                    this.creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
                }
            } else {
                this.creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                this.creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
            }
        }
    }

    @Override
    int getNextTradeId() {
        return ++currentCounter;
    }

    private boolean makeTrade() {
        if ((!this.creatureOne.isPlayer() || this.creatureOne.hasLink()) && !this.creatureOne.isDead()) {
            if ((!this.creatureTwo.isPlayer() || this.creatureTwo.hasLink()) && !this.creatureTwo.isDead()) {
                if (this.creatureOneRequestWindow.hasInventorySpace() && this.creatureTwoRequestWindow.hasInventorySpace()) {
                    int reqOneWeight = this.creatureOneRequestWindow.getWeight();
                    int reqTwoWeight = this.creatureTwoRequestWindow.getWeight();
                    int diff = reqOneWeight - reqTwoWeight;
                    if (diff > 0 && this.creatureOne instanceof Player && !this.creatureOne.canCarry(diff)) {
                        this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " cannot carry that much.", (byte)3);
                        this.creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        if (this.creatureOne.getPower() > 0) {
                            this.creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much. You would carry " + diff + " more.");
                        }

                        return false;
                    }

                    diff = reqTwoWeight - reqOneWeight;
                    if (diff > 0 && this.creatureTwo instanceof Player && !this.creatureTwo.canCarry(diff)) {
                        this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " cannot carry that much.", (byte)3);
                        this.creatureTwo.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        return false;
                    }



                    boolean ok = this.creatureOneRequestWindow.validateTrade();
                    if (!ok) {
                        return false;
                    }

                    ok = this.creatureTwoRequestWindow.validateTrade();
                    if (ok) {
                        this.creatureOneRequestWindow.swapOwners();
                        this.creatureTwoRequestWindow.swapOwners();
                        this.creatureTwoOfferWindow.endTrade();
                        this.creatureOneOfferWindow.endTrade();
                        if (StatTraderTemplate.is(creatureTwo)) {
                            Shop shop = Economy.getEconomy().getShop(creatureTwo);
                            shop.setMerchantData(creatureTwo.getNumberOfShopItems());
                            CustomTraderDatabase.restock(creatureTwo);
                        }

                        this.creatureOne.setTrade(null);
                        this.creatureTwo.setTrade(null);
                        return true;
                    }
                }

                return false;
            } else {
                if (this.creatureTwo.hasLink()) {
                    this.creatureTwo.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
                }

                this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " cannot trade right now.", (byte)3);
                this.end(this.creatureTwo, false);
                return true;
            }
        } else {
            if (this.creatureOne.hasLink()) {
                this.creatureOne.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
            }

            this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " cannot trade right now.", (byte)3);
            this.end(this.creatureOne, false);
            return true;
        }
    }

    @Override
    public void end(Creature creature, boolean closed) {
        try {
            if (creature.equals(this.creatureOne)) {
                this.creatureTwo.getCommunicator().sendCloseTradeWindow();
                if (!closed) {
                    this.creatureOne.getCommunicator().sendCloseTradeWindow();
                }

                this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " withdrew from the trade.", (byte)2);
                this.creatureOne.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
            } else {
                this.creatureOne.getCommunicator().sendCloseTradeWindow();
                if (!closed || !this.creatureTwo.isPlayer()) {
                    this.creatureTwo.getCommunicator().sendCloseTradeWindow();
                }

                this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " withdrew from the trade.", (byte)2);
                this.creatureTwo.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
            }
        } catch (Exception var4) {
            logger.log(Level.WARNING, var4.getMessage(), var4);
        }

        this.creatureTwoOfferWindow.endTrade();
        this.creatureOneOfferWindow.endTrade();
        this.creatureOneRequestWindow.endTrade();
        this.creatureTwoRequestWindow.endTrade();
        this.creatureOne.setTrade(null);
        this.creatureTwo.setTrade(null);
    }

    @Override
    boolean isCreatureOneSatisfied() {
        return this.creatureOneSatisfied;
    }

    @Override
    void setCreatureOneSatisfied(boolean aCreatureOneSatisfied) {
        this.creatureOneSatisfied = aCreatureOneSatisfied;
    }

    @Override
    boolean isCreatureTwoSatisfied() {
        return this.creatureTwoSatisfied;
    }

    @Override
    void setCreatureTwoSatisfied(boolean aCreatureTwoSatisfied) {
        this.creatureTwoSatisfied = aCreatureTwoSatisfied;
    }

    @Override
    public int getCurrentCounter() {
        return this.currentCounter;
    }

    @Override
    void setCurrentCounter(int aCurrentCounter) {
        this.currentCounter = aCurrentCounter;
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
        return this.creatureOneRequestWindow;
    }

    @Override
    public TradingWindow getCreatureTwoRequestWindow() {
        return this.creatureTwoRequestWindow;
    }

    @Override
    Creature getCreatureOne() {
        return this.creatureOne;
    }

    @Override
    Creature getCreatureTwo() {
        return this.creatureTwo;
    }
}
