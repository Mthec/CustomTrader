package com.wurmonline.server.creatures;

import com.wurmonline.server.items.*;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyTraderTradeHandler extends TradeHandler {
    private static final Logger logger = Logger.getLogger(CurrencyTraderTradeHandler.class.getName());
    private final Creature trader;
    private final Trade trade;
    private final ItemTemplate currency;
    private final ArrayList<StockItem> prices = new ArrayList<>();
    private boolean balanced = false;
    private boolean waiting = false;

    public CurrencyTraderTradeHandler(Creature trader, Trade trade) {
        this.trader = trader;
        this.trade = trade;
        int curr = CustomTraderDatabase.getCurrencyFor(trader);
        ItemTemplate template;
        try {
            template = ItemTemplateFactory.getInstance().getTemplate(curr);
        } catch (NoSuchTemplateException e) {
            e.printStackTrace();
            template = null;
        }
        currency = template;

        for (StockInfo info : CustomTraderDatabase.getStockFor(trader)) {
            prices.add(info.item);
        }

        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will trade each of my goods in exchange for " + template.getPlural() + ".'");
    }

    @Override
    void addToInventory(Item item, long inventoryWindow) {
        if (this.trade != null) {
            if (inventoryWindow == 2L) {
                this.tradeChanged();
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to his offer window.");
                }
            } else if (inventoryWindow == 1L) {
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to my offer window.");
                }
            } else if (inventoryWindow == 3L) {
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to his request window.");
                }
            } else if (inventoryWindow == 4L && logger.isLoggable(Level.FINEST) && item != null) {
                logger.finest("Added " + item.getName() + " to my request window.");
            }
        }

    }

    @Override
    public void addItemsToTrade() {
        if (currency == null) {
            trade.creatureOne.getCommunicator().sendAlertServerMessage(trader.getName() + " looks confused and ends the trade.");
            trade.end(trader, true);
            return;
        }

        if (trade != null) {
            TradingWindow myOffers = trade.getTradingWindow(1);
            myOffers.startReceivingItems();

            for (Item item : trader.getInventory().getItems()) {
                myOffers.addItem(item);
            }

            myOffers.stopReceivingItems();
        }
    }

    @Override
    public int getTraderSellPriceForItem(Item item, TradingWindow window) {
        int price = 0;
        for (StockItem stockItem : prices) {
            if (stockItem.matches(item)) {
                price = stockItem.price;
                break;
            }
        }
        return price;
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
    public void balance() {
        if (!balanced) {
            if (!waiting) {
                suckCurrency();
                TradingWindow offerWindow = trade.getTradingWindow(2);
                TradingWindow sellWindow = trade.getTradingWindow(3);
                TradingWindow requestWindow = trade.getTradingWindow(4);
                int diff = Arrays.stream(sellWindow.getAllItems()).mapToInt(i -> getTraderSellPriceForItem(i, sellWindow)).sum() - requestWindow.getAllItems().length;
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("diff is " + diff);
                }

                if (diff > 0) {
                    waiting = true;
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " demands " + diff + " more " + (diff == 1 ? currency.getName() : currency.getPlural()) + " to make the trade.");
                } else if (diff < 0) {
                    while (diff < 0) {
                        Item item = requestWindow.getAllItems()[0];
                        requestWindow.removeItem(item);
                        offerWindow.addItem(item);
                        ++diff;
                    }

                    trade.setSatisfied(trader, true, trade.getCurrentCounter());
                    balanced = true;
                } else {
                    trade.setSatisfied(trader, true, trade.getCurrentCounter());
                    balanced = true;
                }
            }
        }
    }

    private void suckCurrency() {
        TradingWindow offerWindow = trade.getTradingWindow(2);
        TradingWindow requestWindow = trade.getTradingWindow(4);

        for (Item item : offerWindow.getAllItems()) {
            if (item.getTemplate() == currency) {
                offerWindow.removeItem(item);
                requestWindow.addItem(item);
            }
        }
    }
}
