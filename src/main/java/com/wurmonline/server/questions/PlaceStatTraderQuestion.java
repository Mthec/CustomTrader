package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.StatTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class PlaceStatTraderQuestion extends PlaceOrManageTraderQuestion {
    private static final Random r = new Random();
    private final VolaTile tile;
    private final int floorLevel;
    private Template template;
    private final String[] stats = Stat.getAll();

    public PlaceStatTraderQuestion(Creature performer, VolaTile tile, int floorLevel) {
        super(performer, "Set Up Stat Trader", -10);
        this.tile = tile;
        this.floorLevel = floorLevel;
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        int newStatIndex = getIntegerOrDefault("stat", -1);
        String newStat;
        try {
            newStat = stats[newStatIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            newStat = stats[0];
            responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so selected " + newStat + ".");
        }

        float ratio;
        try {
            ratio = getFloatOrDefault("ratio", 1.0f);
            if (ratio <= 0) {
                responder.getCommunicator().sendSafeServerMessage("Ratio must be greater than 0, setting 1.0.");
                ratio = 1.0f;
            }
        } catch (NumberFormatException e) {
            ratio = 1.0f;
            responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so set a ratio of " + ratio + ".");
        }

        Stat stat = Stat.create(newStat, ratio);

        if (stat == null) {
            responder.getCommunicator().sendNormalServerMessage("Something went wrong and the trader was not created.");
            return;
        }

        byte sex = getGender();
        String name = getName(sex);
        String tag = getTag();

        if (locationIsValid(responder, tile)) {
            try {
                Creature trader = StatTraderTemplate.createNewTrader(tile, floorLevel, getPrefix() + name, sex, responder.getKingdomId(), stat, tag);
                logger.info(responder.getName() + " created a stat trader: " + trader.getWurmId());
            } catch (Exception e) {
                responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendQuestion() {
        BML bml = new BMLBuilder(id)
                             .text("Place Stat Trader").bold()
                             .text("Place a trader with a custom inventory that will restock on a schedule.")
                             .text("This trader will take a certain type of stat (e.g. karma, favor, etc.) in exchange for goods.");
        bml = middleBML(bml, "")
                             .text("Stat:")
                             .dropdown("stat", Arrays.asList(stats), 0)
                             .newLine()
                             .text("How many of stat is worth 1i.  e.g. using karma, to buy a 5i item with a ratio of 0.5 it would take 10 karma.")
                             .harray(b -> b.label("Ratio:").entry("ratio", "1.0", 6))
                             .newLine();

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(bml), 200, 200, 200, title);
    }
}
