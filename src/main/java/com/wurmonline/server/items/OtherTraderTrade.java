package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.function.Supplier;

public abstract class OtherTraderTrade<V> extends BaseTrade<V> {
    protected OtherTraderTrade(Creature player, Creature trader, Supplier<V> value) {
        super(player, trader, value);
    }

    @Override
    protected void onSuccessfulTrade(Creature creature) {
        if (CustomTraderMod.isOtherTrader(creatureTwo)) {
            Shop shop = Economy.getEconomy().getShop(creatureTwo);
            shop.setMerchantData(creatureTwo.getNumberOfShopItems());
            CustomTraderDatabase.restock(creatureTwo);
        }
    }
}
