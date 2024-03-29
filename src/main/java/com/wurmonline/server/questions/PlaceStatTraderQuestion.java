package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.StatTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stats.StatFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import static com.wurmonline.server.questions.StatTraderManagementQuestion.STAT_DESCRIPTION;

public class PlaceStatTraderQuestion extends PlaceOrManageTraderQuestion {
    private static final Random r = new Random();
    private final VolaTile tile;
    private final int floorLevel;
    private Template template;
    private final List<StatFactory> stats = Stat.getAll();

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
        StatFactory newStat;
        try {
            newStat = stats.get(newStatIndex);
        } catch (ArrayIndexOutOfBoundsException e) {
            newStat = stats.get(0);
            responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so selected " + newStat.label() + ".");
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

        Stat stat = newStat.create(ratio);

        if (stat == null) {
            responder.getCommunicator().sendNormalServerMessage("Something went wrong and the trader was not created.");
            return;
        }

        byte sex = getGender();
        String name = getName(sex);
        String tag = getTag();

        if (locationIsValid(responder, tile)) {
            try {
                Creature trader = withTempFace(() -> StatTraderTemplate.createNewTrader(tile, floorLevel, name, sex, responder.getKingdomId(), stat, tag), responder.getWurmId());
                logger.info(responder.getName() + " created a stat trader: " + trader.getWurmId());
                checkCustomise(trader);
            } catch (SQLException e) {
                responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was created, but their appearance was not set.");
                e.printStackTrace();
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
                             .dropdown("stat", stats.stream().map(StatFactory::label).collect(Collectors.joining(",")), 0)
                             .newLine()
                             .text(STAT_DESCRIPTION)
                             .harray(b -> b.label("Ratio:").entry("ratio", "1.0", 6))
                             .newLine();

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(bml), 200, 200, 200, title);
    }
}
