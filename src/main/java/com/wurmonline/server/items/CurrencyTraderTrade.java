package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;

import java.util.function.Supplier;

public class CurrencyTraderTrade extends OtherTraderTrade<Void> {
    public CurrencyTraderTrade(Creature player, Creature trader) {
        super(player, trader, () -> null);
    }

    @Override
    protected CurrencyTradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, Supplier<Void> value) {
        return new CurrencyTradingWindow(owner, watcher, offer, wurmId, this);
    }
}
