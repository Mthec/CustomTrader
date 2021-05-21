package com.wurmonline.server.behaviours;

import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CurrencyTraderTradeHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.CurrencyTraderTrade;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.villages.Village;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CurrencyTraderTradeAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(CurrencyTraderTradeAction.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;

    public CurrencyTraderTradeAction() {
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Trade", "trading", ItemBehaviour.emptyIntArr).build();
        ModActions.registerAction(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        if (CurrencyTraderTemplate.isCurrencyTrader(target)) {
            return Collections.singletonList(actionEntry);
        }
        return null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId && CurrencyTraderTemplate.isCurrencyTrader(target)) {
            if (performer.getVehicle() != -10L && !performer.isVehicleCommander()) {
                return true;
            }

            if (target.isFighting()) {
                performer.getCommunicator().sendNormalServerMessage(target.getName() + " is too busy fighting!");
            } else if (performer.isTrading()) {
                performer.getCommunicator().sendNormalServerMessage("You are already trading with someone.");
            } else if (target.isTrading() && !target.shouldStopTrading(true)) {
                Trade trade = target.getTrade();
                if (trade != null) {
                    Creature oppos = trade.creatureOne;
                    if (target.equals(oppos)) {
                        oppos = trade.creatureTwo;
                    }

                    String name = "someone";
                    if (oppos != null) {
                        name = oppos.getName();
                    }

                    performer.getCommunicator().sendNormalServerMessage(target.getName() + " is already trading with " + name + ".");
                }
            } else {
                if (target.getFloorLevel() != performer.getFloorLevel() && performer.getPower() <= 0) {
                    performer.getCommunicator().sendNormalServerMessage("You can't reach " + target.getName() + " there.");
                    return true;
                }

                if (!performer.isFriendlyKingdom(target.getKingdomId()) && performer.getPower() <= 0) {
                    boolean ok = false;
                    Village v;
                    if (Servers.localServer.PVPSERVER && Servers.localServer.isChallengeOrEpicServer() && !Servers.localServer.HOMESERVER) {
                        v = target.getCurrentVillage();
                        if (v != null && v.getGuards().length > 0) {
                            performer.getCommunicator().sendNormalServerMessage("There are guards in the vicinity. You can't start trading with " + target.getName() + " now.");
                            return true;
                        }

                        Shop shop = Economy.getEconomy().getShop(target);
                        if (shop != null) {
                            ok = true;
                        }
                    }

                    if (!ok) {
                        performer.getCommunicator().sendNormalServerMessage(target.getName() + " snorts and refuses to trade with you.");
                        return true;
                    }
                }

                target.turnTowardsCreature(performer);

                try {
                    target.getStatus().savePosition(target.getWurmId(), false, target.getStatus().getZoneId(), true);
                } catch (IOException ignored) {
                }

                Trade trade = new CurrencyTraderTrade(performer, target);
                performer.setTrade(trade);
                target.setTrade(trade);
                target.getCommunicator().sendStartTrading(performer);
                performer.getCommunicator().sendStartTrading(target);
                //noinspection ConstantConditions
                ((CurrencyTraderTradeHandler)target.getTradeHandler()).addItemsToTrade();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item subject, Creature target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
