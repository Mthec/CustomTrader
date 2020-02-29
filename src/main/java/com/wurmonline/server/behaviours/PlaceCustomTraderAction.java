package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.PlaceCustomTraderQuestion;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PlaceCustomTraderAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(PlaceCustomTraderAction.class.getName());
    private final short actionId;
    private final List<ActionEntry> actionEntries;

    public PlaceCustomTraderAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Custom Trader", "placing custom trader").build();
        ModActions.registerAction(actionEntry);

        actionEntries = new ArrayList<>();
        actionEntries.add(new ActionEntry((short)-1, "Place Npc", "placing npcs", ItemBehaviour.emptyIntArr));
        actionEntries.add(actionEntry);
    }

    private List<ActionEntry> getBehaviours(Creature performer, Item item) {
        if (item.isWand() && performer.getPower() >= 2)
            return actionEntries;
        return null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile) {
        return getBehaviours(performer, item);
    }

    // Seems to be for inside mines.  What is the point of onSurface in other methods then?
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile, int dir) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, Floor floor) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, BridgePart bridgePart) {
        return getBehaviours(performer, item);
    }

    private boolean doAction(short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        if (num == actionId && source.isWand() && performer.getPower() >= 2) {
            new PlaceCustomTraderQuestion(performer, tile, floorLevel).sendQuestion();
            return true;
        }
        return false;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int floorLevel, int tile, short num, float counter) {
        VolaTile volaTile = Zones.getOrCreateTile(tilex, tiley, onSurface);
        if (volaTile == null) {
            performer.getCommunicator().sendAlertServerMessage("You could not be located.");
            logger.warning("Could not find or create tile (" + tile + ") at " + tilex + " - " + tiley + " surfaced=" + onSurface);
            return true;
        }

        if (!onSurface)
            floorLevel = 0;

        return doAction(num, performer, source, volaTile, floorLevel);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
        return doAction(num, performer, source, floor.getTile(), floor.getFloorLevel());
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
        return doAction(num, performer, source, bridgePart.getTile(), bridgePart.getFloorLevel());
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
