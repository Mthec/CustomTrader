package com.wurmonline.server.questions;

import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.StatTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class PlaceStatTraderQuestion extends CustomTraderQuestionExtension {
    private static final Random r = new Random();
    private final VolaTile tile;
    private final int floorLevel;
    private Template template;
    private final String[] stats = Stat.getAll();

    public PlaceStatTraderQuestion(Creature performer, VolaTile tile, int floorLevel) {
        super(performer, "Set Up Stat Trader", "", MANAGETRADER, -10);
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

        byte sex = 0;
        if (wasAnswered("gender", "female"))
            sex = 1;

        String name = StringUtilities.raiseFirstLetter(getStringProp("name"));
        if (name.isEmpty() || name.length() > 20 || QuestionParser.containsIllegalCharacters(name)) {
            if (sex == 0) {
                name = QuestionParser.generateGuardMaleName();
                responder.getCommunicator().sendSafeServerMessage("The trader didn't like the name, so he chose a new one.");
            } else {
                name = QuestionParser.generateGuardFemaleName();
                responder.getCommunicator().sendSafeServerMessage("The trader didn't like the name, so she chose a new one.");
            }
        }

        String tag = getStringProp("tag");
        if (tag.length() > CustomTraderMod.maxTagLength) {
            responder.getCommunicator().sendSafeServerMessage("The tag was too long, so it was cut short.");
            tag = tag.substring(0, CustomTraderMod.maxTagLength);
        }

        if (locationIsValid(responder)) {
            try {
                Creature trader = StatTraderTemplate.createNewTrader(tile, floorLevel, getPrefix() + name, sex, responder.getKingdomId(), stat, tag);
                logger.info(responder.getName() + " created a stat trader: " + trader.getWurmId());
            } catch (Exception e) {
                responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
                e.printStackTrace();
            }
        }
    }

    private boolean locationIsValid(Creature responder) {
        if (tile != null) {
            if (!Methods.isActionAllowed(responder, Actions.MANAGE_TRADERS)) {
                return false;
            }
            for (Creature creature : tile.getCreatures()) {
                if (!creature.isPlayer()) {
                    responder.getCommunicator().sendNormalServerMessage("The trader will only set up shop where no other creatures except you are standing.");
                    return false;
                }
            }

            Structure struct = tile.getStructure();
            if (struct != null && !struct.mayPlaceMerchants(responder)) {
                responder.getCommunicator().sendNormalServerMessage("You do not have permission to place a trader in this building.");
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendQuestion() {
        boolean gender = r.nextBoolean();

        String bml = new BMLBuilder(id)
                             .text("Place Stat Trader").bold()
                             .text("Place a trader with a custom inventory that will restock on a schedule.")
                             .text("This trader will take a certain type of stat (e.g. karma, favour, etc.) in exchange for goods.")
                             .text("Use a 'tag' to use the same inventory contents for multiple stat traders.")
                             .newLine()
                             .harray(b -> b.label("Name: " + getPrefix()).entry("name", CustomTraderMod.maxNameLength))
                             .text("Leave blank for a random name.").italic()
                             .newLine()
                             .text("Stat:")
                             .dropdown("stat", Arrays.asList(stats), 0)
                             .newLine()
                             .text("How many of stat is worth 1i.  e.g. using karma, to buy a 5i item with a ratio of 0.5 it would take 10 karma.")
                             .harray(b -> b.label("Ratio:").entry("ratio", "1.0", 6))
                             .newLine()
                             .harray(b -> b.label("Tag:").entry("tag", CustomTraderMod.maxTagLength))
                             .text("Leave blank to create a unique trader.").italic()
                             .newLine()
                             .text("Gender:")
                             .radio("gender", "male", "Male", gender)
                             .radio("gender", "female", "Female", !gender)
                             .newLine()
                             .harray(b -> b.button("Send"))
                             .build();

        getResponder().getCommunicator().sendBml(450, 400, true, true, bml, 200, 200, 200, title);
    }
}
