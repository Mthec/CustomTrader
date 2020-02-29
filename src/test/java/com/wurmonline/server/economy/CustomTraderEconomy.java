package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;

public class CustomTraderEconomy {
    public static void createShop(long wurmid) {
        CustomTraderObjectsFactory factory = ((CustomTraderObjectsFactory)CustomTraderObjectsFactory.getCurrent());
        try {
            Creature trader = factory.getCreature(wurmid);
            factory.addShop(trader, FakeShop.createFakeTraderShop(wurmid));
        } catch (NoSuchCreatureException e) {
            throw new RuntimeException(e);
        }
    }
}

