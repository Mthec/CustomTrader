package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stats.StatFactory;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class StatTraderManagementQuestion extends PlaceOrManageTraderQuestion {
    static final String STAT_DESCRIPTION = "How much one unit of stat is worth 1i.  e.g. using karma, to buy a 5i item with a ratio of 0.5 it would take 10 karma.";
    private final Creature trader;
    private final String currentTag;
    private final List<StatFactory> stats = Stat.getAll();
    private final Stat currentStat;

    public StatTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Karma Trader", trader);
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
            checkSaveFace(trader);
            checkSaveModel(trader);

            int newStatIndex = getIntegerOrDefault("stat", -1);
            StatFactory newStat;
            try {
                newStat = stats.get(newStatIndex);
            } catch (ArrayIndexOutOfBoundsException e) {
                newStat = currentStat.getFactory();
                responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so selected " + currentStat.label() + ".");
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
            if (!newStat.label().equals(currentStat.name) || ratio != currentStat.ratio) {
                stat = newStat.create(ratio);
            } else {
                stat = currentStat;
            }

            if (stat == null) {
                responder.getCommunicator().sendNormalServerMessage("Something went wrong and the trader was not created.");
                return;
            } else if (stat != currentStat) {
                CustomTraderDatabase.setStatFor(trader, stat);
                responder.getCommunicator().sendNormalServerMessage("The trader will now collect " + stat.label() + " at a ratio of " + stat.ratio + ":1.");
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
        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(trader.getName()))
                     .text("Stat:")
                     .dropdown("stat", stats.stream().map(StatFactory::label).collect(Collectors.joining(",")), stats.indexOf(currentStat.getFactory()))
                     .newLine()
                     .text(STAT_DESCRIPTION)
                     .harray(b -> b.label("Ratio:").entry("ratio", Float.toString(currentStat.ratio), 6))
                     .newLine();

        getResponder().getCommunicator().sendBml(400, 350, true, true, endBML(bml, currentTag, trader), 200, 200, 200, title);
    }
}
