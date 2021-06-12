package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatTraderTradeHandler extends TradeHandler {
    private static final Logger logger = Logger.getLogger(StatTraderTradeHandler.class.getName());
    private final Creature trader;
    private final Trade trade;
    private final Stat stat;
    private boolean balanced = false;
    private boolean waiting = false;

    public StatTraderTradeHandler(Creature trader, Trade trade) {
        super(trader, trade);
        this.trader = trader;
        this.trade = trade;
        this.stat = CustomTraderDatabase.getStatFor(trader);

        if (stat == null) {
            trade.creatureOne.getCommunicator().sendAlertServerMessage(trader.getName() + " looks confused and ends the trade.");
            trade.end(trader, true);
        } else {
            trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will trade my goods in exchange for " + stat.name + ", " +
                                                                              "I see you have " + stat.creatureHas(trade.creatureOne) + ".'");
        }
    }

    @Override
    void addItemsToTrade() {}

    @Override
    public int getTraderSellPriceForItem(Item item, TradingWindow window) {
        return (int)(item.getPrice() * stat.ratio);
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
                TradingWindow sellWindow = trade.getTradingWindow(3);
                int diff = Arrays.stream(sellWindow.getAllItems()).mapToInt(i -> getTraderSellPriceForItem(i, sellWindow)).sum() -
                           trade.creatureOne.getKarma();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("diff is " + diff);
                }

                if (diff > 0L) {
                    waiting = true;
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " demands " + diff + " " + stat.name + " to make the trade.");
                } else if (diff < 0L) {
                    trade.setSatisfied(trader, true, this.trade.getCurrentCounter());
                    balanced = true;
                } else {
                    trade.setSatisfied(trader, true, trade.getCurrentCounter());
                    balanced = true;
                }
            }
        }
    }
}
