package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.PlaceCustomTraderQuestion;
import com.wurmonline.server.zones.VolaTile;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class PlaceCustomTraderAction extends PlaceSpecialTraderActions {
    private final short actionId;

    public PlaceCustomTraderAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Custom Trader", "placing custom trader").build();
        PlaceSpecialTraderActions.actionEntries.add(actionEntry);
        ModActions.registerAction(actionEntry);
    }

    @Override
    protected boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        if (num == actionId && source.isWand() && performer.getPower() >= 2) {
            new PlaceCustomTraderQuestion(performer, tile, floorLevel).sendQuestion();
            return true;
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
