package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.CurrencyTraderManagementQuestion;
import com.wurmonline.server.questions.CustomTraderManagementQuestion;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

public class ManageCustomTraderAction implements ModAction, BehaviourProvider, ActionPerformer {
    private final short actionId;
    private final ActionEntry actionEntry;

    public ManageCustomTraderAction() {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = new ActionEntryBuilder(actionId, "Manage", "managing custom trader").build();
        ModActions.registerAction(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        if (subject.isWand() && performer.getPower() >= 2 && (CustomTraderTemplate.isCustomTrader(target) || CurrencyTraderTemplate.isCurrencyTrader(target)))
            return Collections.singletonList(actionEntry);
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (num == actionId && source.isWand() && performer.getPower() >= 2) {
            if (CustomTraderTemplate.isCustomTrader(target)) {
                new CustomTraderManagementQuestion(performer, target).sendQuestion();
            } else if (CurrencyTraderTemplate.isCurrencyTrader(target)) {
                new CurrencyTraderManagementQuestion(performer, target).sendQuestion();
            }
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
