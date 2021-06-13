package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class StatTradingWindow extends OtherTraderTradingWindow<StatTraderTrade> {
    private static final Logger logger = Logger.getLogger(StatTradingWindow.class.getName());
    private final Stat stat;
    private boolean problems;

    StatTradingWindow(Creature owner, Creature watcher, boolean offer, long wurmId, StatTraderTrade trade, Supplier<Stat> value) {
        super(owner, watcher, offer, wurmId, trade);
        stat = value.get();
    }

    @Override
    boolean validateTrade() {
        if (windowOwner.isDead()) {
            return false;
        } else if (windowOwner instanceof Player && !windowOwner.hasLink()) {
            return false;
        } else {
            if (items != null) {
                int paymentRequired = 0;
                for (Item item : items) {
                    if (itemNotValid(item)) {
                        return false;
                    }

                    for (Item containedItem : item.getAllItems(false)) {
                        if (itemNotValid(containedItem)) {
                            return false;
                        }

                        paymentRequired += containedItem.getPrice();
                    }

                    paymentRequired += item.getPrice();
                }

                if (watcher instanceof Player) {
                    if (paymentRequired > stat.creatureHas(watcher)) {
                        windowOwner.getCommunicator().sendSafeServerMessage("Player does not have enough " + stat.name + ". Trade aborted.");
                        watcher.getCommunicator().sendSafeServerMessage("You do not have enough " + stat.name + ". Trade aborted.");

                        return false;
                    }
                }
            }

            return true;
        }
    }

    @Override
    protected int handleItemTransfer(Item item) {
        // Window 3
        if (watcher instanceof Player) {
            int amount = item.getPrice();
            boolean success = stat.takeStatFrom(watcher, amount);
            if (success) {
                watcher.getInventory().insertItem(item);
                return amount;
            } else {
                problems = true;

            }
        }

        return 0;
    }

    @Override
    void swapOwners() {
        problems = false;

        super.swapOwners();

        if (problems) {
            windowOwner.getCommunicator().sendNormalServerMessage("Something went wrong and not all items were traded.");
            watcher.getCommunicator().sendNormalServerMessage("Something went wrong and not all items were traded.");
        }
    }
}
