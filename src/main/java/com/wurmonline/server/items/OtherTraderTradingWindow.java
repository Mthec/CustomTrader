package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;

public abstract class OtherTraderTradingWindow<T extends Trade> extends BaseTradingWindow<T> {
    protected OtherTraderTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, T trade) {
        super(owner, watcher, offer, wurmId, trade);
    }

    @Override
    protected String getLoggerNamePrefix() {
        return "othertrader_";
    }
}
