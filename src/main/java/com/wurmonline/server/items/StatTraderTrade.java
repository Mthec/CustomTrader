package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.function.Supplier;

public class StatTraderTrade extends OtherTraderTrade<Stat> {
    final Stat stat;

    public StatTraderTrade(Creature player, Creature trader, Stat stat) {
        super(player, trader, () -> stat);
        this.stat = stat;
    }

    @Override
    protected TradingWindow createTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, Supplier<Stat> value) {
        return new StatTradingWindow(owner, watcher, offer, wurmId, this, value);
    }
}
