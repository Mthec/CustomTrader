package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.util.logging.Logger;

public class CurrencyTradingWindow extends OtherTraderTradingWindow<CurrencyTraderTrade> {
    private static final Logger logger = Logger.getLogger(CurrencyTradingWindow.class.getName());

    CurrencyTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, CurrencyTraderTrade trade) {
        super(owner, watcher, offer, wurmId, trade);
    }

    @Override
    protected int handleItemTransfer(Item item) {
        // Window 4
        if (!(watcher instanceof Player)) {
            Items.destroyItem(item.getWurmId());
            return 1;
        // Window 3
        } else {
            watcher.getInventory().insertItem(item);
            return 0;
        }
    }
}
