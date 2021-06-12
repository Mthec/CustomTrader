package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;

public class CurrencyTraderTrade extends OtherTraderTrade {
    public CurrencyTraderTrade(Creature player, Creature trader) {
        super(player, trader);
    }

    @Override
    protected TradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId) {
        return new CurrencyTradingWindow(owner, watcher, offer, wurmId, this);
    }
}
