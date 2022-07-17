package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.io.IOException;
import java.util.logging.*;

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

    @SuppressWarnings("DuplicatedCode")
    public Logger getCreatureLogger(Creature creature) {
        String name = getLoggerNamePrefix() + creature.getWurmId();
        Logger personalLogger = loggers.get(name);
        if (personalLogger == null) {
            personalLogger = Logger.getLogger(name);
            personalLogger.setUseParentHandlers(false);
            Handler[] h = logger.getHandlers();

            for(int i = 0; i != h.length; ++i) {
                personalLogger.removeHandler(h[i]);
            }

            try {
                FileHandler fh = new FileHandler(name + ".log", 0, 1, true);
                fh.setFormatter(new SimpleFormatter());
                personalLogger.addHandler(fh);
            } catch (IOException var6) {
                Logger.getLogger(name).log(Level.WARNING, name + ":no redirection possible!");
            }

            loggers.put(name, personalLogger);
        }

        return personalLogger;
    }
}
