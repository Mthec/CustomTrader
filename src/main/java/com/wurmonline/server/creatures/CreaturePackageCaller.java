package com.wurmonline.server.creatures;

import java.io.IOException;

public class CreaturePackageCaller {
    public static void saveCreatureName(Creature trader, String name) throws IOException {
        trader.getStatus().saveCreatureName(name);
        trader.setName(name);
    }
}
