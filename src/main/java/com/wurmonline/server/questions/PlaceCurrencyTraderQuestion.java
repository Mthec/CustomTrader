package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CurrencyTraderTemplate;

import java.util.Properties;

public class PlaceCurrencyTraderQuestion extends PlaceOrManageTraderQuestion {

    private final VolaTile tile;
    private final int floorLevel;
    private Template template;

    public PlaceCurrencyTraderQuestion(Creature performer, VolaTile tile, int floorLevel) {
        super(performer, "Set Up Currency Trader", -10);
        this.tile = tile;
        this.floorLevel = floorLevel;
        EligibleTemplates.init();
        template = Template._default();
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

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

            byte sex = getGender();
            String name = getName(sex);
            String tag = getTag();

            if (locationIsValid(responder, tile)) {
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

    @Override
    public void sendQuestion() {
        BML bml = new BMLBuilder(id)
                             .text("Place Currency Trader").bold()
                             .text("Place a trader with a custom inventory that will restock on a schedule.")
                             .text("This trader will only accept a certain type of item in exchange for goods.");
        bml = middleBML(bml, "")
                             .text("Currency:")
                             .text("Filter available templates:")
                             .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                             .newLine()
                             .harray(b -> b.dropdown("template", template.getOptions(), template.templateIndex)
                                                  .spacer().label("Filter:")
                                                  .entry("filter", template.filter, 10).spacer()
                                                  .button("do_filter", "Apply"))
                             .newLine();

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(bml), 200, 200, 200, title);
    }
}
