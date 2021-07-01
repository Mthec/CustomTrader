package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.Properties;

public class CustomTraderManagementQuestion extends PlaceOrManageTraderQuestion {
    private final Creature trader;
    private final String currentTag;

    public CustomTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Custom Trader", trader);
        this.trader = trader;
        currentTag = CustomTraderDatabase.getTagFor(trader);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        if (wasSelected("confirm")) {
            checkSaveName(trader);
            checkSaveTag(trader, currentTag);
            checkStockOptions(trader);
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader, PaymentType.coin).sendQuestion();
        } else if (wasSelected("dismiss")) {
            tryDismiss(trader, "custom");
        } else {
            checkCustomise(trader);
        }
    }

    @Override
    public void sendQuestion() {
        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(trader.getName()));

        getResponder().getCommunicator().sendBml(400, 350, true, true, endBML(bml, currentTag, trader), 200, 200, 200, title);
    }
}
