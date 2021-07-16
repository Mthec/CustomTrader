package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTemplate;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomTraderTradeHandler extends TradeHandler {
    private static final Logger logger = Logger.getLogger(CustomTraderTradeHandler.class.getName());
    private final Creature trader;
    private final Trade trade;
    private boolean balanced = false;
    private boolean waiting = false;

    public CustomTraderTradeHandler(Creature trader, Trade trade) {
        super(trader, trade);
        this.trader = trader;
        this.trade = trade;
    }

    @Override
    void addItemsToTrade() {
        if (trade != null) {
            TradingWindow myOffers = trade.getTradingWindow(1L);
            myOffers.startReceivingItems();

            for (Item item : trader.getInventory().getItems()) {
                myOffers.addItem(item);
            }

            myOffers.stopReceivingItems();
        }
    }

    @Override
    public int getTraderSellPriceForItem(Item item, TradingWindow window) {
        return item.getPrice();
    }

    @Override
    public int getTraderBuyPriceForItem(Item item) {
        return 0;
    }

    @Override
    void tradeChanged() {
        balanced = false;
        waiting = false;
    }

    @Override
    void balance() {
        if (!balanced) {
            if (!waiting) {
                suckCoins();
                TradingWindow sellWindow = trade.getTradingWindow(3);
                TradingWindow requestWindow = trade.getTradingWindow(4);
                removeCoins(sellWindow);
                int diff = Arrays.stream(sellWindow.getAllItems()).mapToInt(i -> getTraderSellPriceForItem(i, sellWindow)).sum() -
                           Arrays.stream(requestWindow.getAllItems()).mapToInt(Item::getValue).sum();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("diff is " + diff);
                }

                if (diff > 0L) {
                    waiting = true;
                    Change change = new Change(diff);
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " demands " + change.getChangeString() + " coins to make the trade.");
                } else if (diff < 0L) {
                    Item[] money = Economy.getEconomy().getCoinsFor(Math.abs(diff));
                    sellWindow.startReceivingItems();

                    for (Item item : money) {
                        sellWindow.addItem(item);
                    }

                    sellWindow.stopReceivingItems();
                    trade.setSatisfied(trader, true, this.trade.getCurrentCounter());
                    balanced = true;
                } else {
                    trade.setSatisfied(trader, true, trade.getCurrentCounter());
                    balanced = true;
                }
            }
        }
    }

    private void suckCoins() {
        TradingWindow offerWindow = trade.getTradingWindow(2);
        TradingWindow requestWindow = trade.getTradingWindow(4);

        for (Item item : offerWindow.getAllItems()) {
            if (item.isCoin()) {
                offerWindow.removeItem(item);
                requestWindow.addItem(item);
            }
        }
    }

    private void removeCoins(TradingWindow sellWindow) {
        for (Item item : sellWindow.getAllItems()) {
            if (item.isCoin()) {
                sellWindow.removeItem(item);
            }
        }
    }

    public static TradeHandler create(Creature creature, Trade trade) {
        if (CustomTraderTemplate.isCustomTrader(creature)) {
            return new CustomTraderTradeHandler(creature, trade);
        } else if (CurrencyTraderTemplate.isCurrencyTrader(creature)) {
            return new CurrencyTraderTradeHandler(creature, trade);
        } else {
            return new StatTraderTradeHandler(creature, trade);
        }
    }
}
