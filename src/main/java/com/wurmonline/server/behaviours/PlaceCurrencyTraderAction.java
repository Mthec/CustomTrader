package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.PlaceCurrencyTraderQuestion;
import com.wurmonline.server.zones.VolaTile;

public class PlaceCurrencyTraderAction implements NpcMenuEntry {
    private final static String name = "Currency Trader";

    public PlaceCurrencyTraderAction() {
        PlaceNpcMenu.addNpcAction(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        new PlaceCurrencyTraderQuestion(performer, tile, floorLevel).sendQuestion();
        return true;
    }
}
