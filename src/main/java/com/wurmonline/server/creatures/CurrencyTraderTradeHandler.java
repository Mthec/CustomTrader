package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Materials;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.questions.WeightHelper;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyTraderTradeHandler extends TradeHandler {
    private static final Logger logger = Logger.getLogger(CurrencyTraderTradeHandler.class.getName());
    private static final DecimalFormat df = new DecimalFormat("0");
    private final Creature trader;
    private final Trade trade;
    private final Currency currency;
    private final ArrayList<StockItem> prices = new ArrayList<>();
    private boolean balanced = false;
    private boolean waiting = false;
    private final Set<Currency.MatchStatus> sentMessages = new HashSet<>();
    public final boolean aborted;

    public CurrencyTraderTradeHandler(Creature trader, Trade trade) {
        boolean tempAborted;
        this.trader = trader;
        this.trade = trade;
        currency = CustomTraderDatabase.getCurrencyFor(trader);
        if (currency != null) {

            for (StockInfo info : CustomTraderDatabase.getStockFor(trader)) {
                prices.add(info.item);
            }

            trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will trade my goods in exchange for " + currency.getPlural() + ".'");
            tempAborted = false;
        } else {
            logger.warning("Currency Trader currency was null when initiating trade.");
            tempAborted = true;
            trade.creatureOne.getCommunicator().sendAlertServerMessage(trader.getName() + " looks confused and ends the trade.");
        }
        aborted = tempAborted;
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
                    trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " demands " + diff + " more " + currency.getNameFor(diff) + " to make the trade.");
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
        sentMessages.clear();

        for (Item item : offerWindow.getAllItems()) {
            switch (currency.matches(item)) {
                case MATCHES:
                    offerWindow.removeItem(item);
                    requestWindow.addItem(item);
                    break;
                case WRONG_TEMPLATE:
                    if (!sentMessages.contains(Currency.MatchStatus.WRONG_TEMPLATE)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getPlural() + ".'");
                        sentMessages.add(Currency.MatchStatus.WRONG_TEMPLATE);
                    }
                    break;
                case QL_TOO_LOW:
                    if (!sentMessages.contains(Currency.MatchStatus.QL_TOO_LOW)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getTemplate().getPlural() + " that are " + df.format(currency.minQL) + "ql or greater.'");
                        sentMessages.add(Currency.MatchStatus.QL_TOO_LOW);
                    }
                    break;
                case QL_DOES_NOT_MATCH_EXACT:
                    if (!sentMessages.contains(Currency.MatchStatus.QL_DOES_NOT_MATCH_EXACT)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getTemplate().getPlural() + " that are exactly " + df.format(currency.exactQL) + "ql.'");
                        sentMessages.add(Currency.MatchStatus.QL_DOES_NOT_MATCH_EXACT);
                    }
                    break;
                case WRONG_MATERIAL:
                    if (!sentMessages.contains(Currency.MatchStatus.WRONG_MATERIAL)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getTemplate().getPlural() + " that are made of " + Materials.convertMaterialByteIntoString(currency.material) + ".'");
                        sentMessages.add(Currency.MatchStatus.WRONG_MATERIAL);
                    }
                    break;
                case WRONG_RARITY:
                    if (!sentMessages.contains(Currency.MatchStatus.WRONG_RARITY)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getTemplate().getPlural() + " that are " + MaterialUtilities.getRarityString(currency.rarity) + ".'");
                        sentMessages.add(Currency.MatchStatus.WRONG_RARITY);
                    }
                    break;
                case DOES_NOT_WEIGH_ENOUGH:
                    if (!sentMessages.contains(Currency.MatchStatus.DOES_NOT_WEIGH_ENOUGH)) {
                        trade.creatureOne.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I will only accept " + currency.getTemplate().getPlural() + " that weigh " + WeightHelper.toString(currency.getTemplate().getWeightGrams()) + "kg.'");
                        sentMessages.add(Currency.MatchStatus.DOES_NOT_WEIGH_ENOUGH);
                    }
                    break;
            }
        }
    }
}
