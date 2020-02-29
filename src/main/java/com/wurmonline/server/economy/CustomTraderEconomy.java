package com.wurmonline.server.economy;


public class CustomTraderEconomy {
    public static void createShop(long wurmid) {
        new DbShop(wurmid, 0);
    }
}
