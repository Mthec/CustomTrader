package com.wurmonline.server.questions;

import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;

import java.util.Properties;
import java.util.Random;

public class PlaceCurrencyTraderQuestion extends CustomTraderQuestionExtension {
    private static final Random r = new Random();
    private final VolaTile tile;
    private final int floorLevel;
    private Template template;

    public PlaceCurrencyTraderQuestion(Creature performer, VolaTile tile, int floorLevel) {
        super(performer, "Set Up Currency Trader", "", MANAGETRADER, -10);
        this.tile = tile;
        this.floorLevel = floorLevel;
        EligibleTemplates.init();
        template = Template._default();
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        if (wasSelected("do_filter")) {
            String filter = getStringOrDefault("filter", "");
            template = new Template(0, filter);

            sendQuestion();
        } else {
            int newTemplateIndex = getIntegerOrDefault("template", template.templateIndex);
            if (newTemplateIndex != template.templateIndex) {
                try {
                    template = new Template(template, newTemplateIndex);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }

            Creature responder = getResponder();

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
                responder.getCommunicator().sendAlertServerMessage("The tag was too long, so it was cut short.");
                tag = tag.substring(0, CustomTraderMod.maxTagLength);
            }

            if (locationIsValid(responder)) {
                try {
                    Creature trader = CurrencyTraderTemplate.createNewTrader(tile, floorLevel, getPrefix() + name, sex, responder.getKingdomId(), template.itemTemplate.getTemplateId(), tag);
                    logger.info(responder.getName() + " created a currency trader: " + trader.getWurmId());
                } catch (Exception e) {
                    responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
                    e.printStackTrace();
                }
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
                             .text("Place Currency Trader").bold()
                             .text("Place a trader with a custom inventory that will restock on a schedule.")
                             .text("This trader will only accept a certain type of item in exchange for goods.")
                             .text("Use a 'tag' to use the same inventory contents for multiple custom/currency traders.")
                             .newLine()
                             .harray(b -> b.label("Name: " + getPrefix()).entry("name", CustomTraderMod.maxNameLength))
                             .text("Leave blank for a random name.").italic()
                             .newLine()
                             .text("Currency:")
                             .text("Filter available templates:")
                             .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                             .newLine()
                             .harray(b -> b.dropdown("template", template.getOptions(), template.templateIndex)
                                                  .spacer().label("Filter:")
                                                  .entry("filter", template.filter, 10).spacer()
                                                  .button("do_filter", "Apply"))
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
