package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;

import java.util.function.Supplier;
import java.util.logging.Level;

public class CurrencyTraderTrade extends OtherTraderTrade<Void> {
    public CurrencyTraderTrade(Creature player, Creature trader) {
        super(player, trader, () -> null);
    }

    @Override
    protected CurrencyTradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, Supplier<Void> value) {
        return new CurrencyTradingWindow(owner, watcher, offer, wurmId, this);
    }

    @Override
    public void setSatisfied(Creature creature, boolean satisfied, int id) {
        if (CurrencyTraderTemplate.isCurrencyTrader(creature) && satisfied) {
            if (getTradingWindow(3).getItems().length > 0 && getTradingWindow(4).getItems().length == 0) {
                ((CurrencyTradingWindow)getTradingWindow(4)).getCreatureLogger(creature).log(Level.WARNING, "Imbalanced trade detected.", new Throwable());
            }
        }
        super.setSatisfied(creature, satisfied, id);
    }
}
