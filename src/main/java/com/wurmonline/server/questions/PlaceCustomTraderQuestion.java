package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTemplate;

import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;

public class PlaceCustomTraderQuestion extends PlaceOrManageTraderQuestion {
    private static final Random r = new Random();
    private final VolaTile tile;
    private final int floorLevel;

    public PlaceCustomTraderQuestion(Creature performer, VolaTile tile, int floorLevel) {
        super(performer, "Set Up Custom Trader", -10);
        this.tile = tile;
        this.floorLevel = floorLevel;
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        byte sex = getGender();
        String name = getName(sex);
        String tag = getTag();

        if (locationIsValid(responder, tile)) {
            try {
                Creature trader = CustomTraderTemplate.createNewTrader(tile, floorLevel, name, sex, responder.getKingdomId(), tag);
                logger.info(responder.getName() + " created a custom trader: " + trader.getWurmId());
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
        BML bml = middleBML(new BMLBuilder(id)
                             .text("Place Custom Trader").bold()
                             .text("Place a trader with a custom inventory that will restock on a schedule."),
                "");

        getResponder().getCommunicator().sendBml(400, 350, true, true, endBML(bml), 200, 200, 200, title);
    }
}
