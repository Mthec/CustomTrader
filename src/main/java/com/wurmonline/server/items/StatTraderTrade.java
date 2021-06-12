package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

public class StatTraderTrade extends OtherTraderTrade {
    final Stat stat;

    public StatTraderTrade(Creature player, Creature trader, Stat stat) {
        super(player, trader);
        this.stat = stat;
    }

    @Override
    protected TradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId) {
        return new StatTradingWindow(owner, watcher, offer, wurmId, this);
    }
}
