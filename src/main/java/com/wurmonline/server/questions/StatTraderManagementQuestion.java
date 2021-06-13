package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class StatTraderManagementQuestion extends PlaceOrManageTraderQuestion {
    private final Creature trader;
    private final String currentTag;
    private final String[] stats = Stat.getAll();
    private final Stat currentStat;

    public StatTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Karma Trader", trader.getWurmId());
        this.trader = trader;
        currentStat = CustomTraderDatabase.getStatFor(trader);
        currentTag = CustomTraderDatabase.getTagFor(trader);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        if (wasSelected("confirm")) {
            checkSaveName(trader);

            int newStatIndex = getIntegerOrDefault("stat", -1);
            String newStat;
            try {
                newStat = stats[newStatIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                newStat = currentStat.name;
                responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so selected " + currentStat.name + ".");
            }

            float ratio;
            try {
                ratio = getFloatOrDefault("ratio", 1.0f);
                if (ratio <= 0) {
                    responder.getCommunicator().sendSafeServerMessage("Ratio must be greater than 0, not changing.");
                    ratio = currentStat.ratio;
                }
            } catch (NumberFormatException e) {
                ratio = currentStat.ratio;
                responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so did not change the ratio.");
            }

            Stat stat;
            if (!newStat.equals(currentStat.name) || ratio != currentStat.ratio) {
                stat = Stat.create(newStat, ratio);
            } else {
                stat = currentStat;
            }

            if (stat == null) {
                responder.getCommunicator().sendNormalServerMessage("Something went wrong and the trader was not created.");
                return;
            } else if (stat != currentStat) {
                CustomTraderDatabase.setStatFor(trader, stat);
                responder.getCommunicator().sendNormalServerMessage("The trader will now collect " + stat.name + " at a ratio of " + stat.ratio + ":1.");
            }
            
            checkSaveTag(trader, currentTag);
            checkStockOptions(trader);
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader, PaymentType.other).sendQuestion();
        } else if (wasSelected("dismiss")) {
            tryDismiss(trader, "stat");
        }
    }

    @Override
    public void sendQuestion() {
        List<String> statsList = Arrays.asList(stats);
        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(trader.getName()))
                     .text("Stat:")
                     .dropdown("stat", statsList, statsList.indexOf(currentStat.name))
                     .newLine()
                     .text("How many of stat is worth 1i.  e.g. using karma, to buy a 10i item with a ratio of 0.5 it would only take 5 karma.")
                     .harray(b -> b.label("Ratio:").entry("ratio", Float.toString(currentStat.ratio), 6))
                     .newLine();

        getResponder().getCommunicator().sendBml(400, 350, true, true, endBML(bml, currentTag, trader), 200, 200, 200, title);
    }
}
