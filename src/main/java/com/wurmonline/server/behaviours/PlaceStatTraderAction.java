package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.PlaceStatTraderQuestion;
import com.wurmonline.server.zones.VolaTile;

public class PlaceStatTraderAction implements NpcMenuEntry {
    private final static String name = "Stat Trader";

    public PlaceStatTraderAction() {
        PlaceNpcMenu.addNpcAction(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        new PlaceStatTraderQuestion(performer, tile, floorLevel).sendQuestion();
        return true;
    }
}
