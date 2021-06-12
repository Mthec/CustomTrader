package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.Properties;

public class CurrencyTraderManagementQuestion extends PlaceOrManageTraderQuestion {
    private final Creature trader;
    private final int currency;
    private final String currentTag;
    private Template template;

    public CurrencyTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Currency Trader", trader.getWurmId());
        this.trader = trader;
        currentTag = CustomTraderDatabase.getTagFor(trader);
        EligibleTemplates.init();
        currency = CustomTraderDatabase.getCurrencyFor(trader);
        template = Template.getForTemplateId(currency);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        if (wasSelected("do_filter")) {
            String filter = getStringOrDefault("filter", "");
            template = new Template(0, filter);

            sendQuestion();
        } else if (wasSelected("confirm")) {
            checkSaveName(trader);

            int newTemplateIndex = getIntegerOrDefault("template", template.templateIndex);
            if (newTemplateIndex != template.templateIndex) {
                try {
                    template = new Template(template, newTemplateIndex);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }

            if (template.itemTemplate.getTemplateId() != currency) {
                CustomTraderDatabase.setCurrencyFor(trader, template.itemTemplate.getTemplateId());
                getResponder().getCommunicator().sendNormalServerMessage(trader.getName() + "'s currency was set to " + template.itemTemplate.getPlural() + ".");
            }

            checkSaveTag(trader, currentTag);
            checkStockOptions(trader);
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader, PaymentType.currency).sendQuestion();
        } else if (wasSelected("dismiss")) {
            tryDismiss(trader, "custom");
        }
    }

    @Override
    public void sendQuestion() {
        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(trader.getName()))
                             .text("Currency:")
                             .text("Filter available templates:")
                             .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                             .newLine()
                             .harray(b -> b.dropdown("template", template.getOptions(), template.templateIndex)
                                                  .spacer().label("Filter:")
                                                  .entry("filter", template.filter, 10).spacer()
                                                  .button("do_filter", "Apply"))
                             .newLine();

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(bml, currentTag, trader), 200, 200, 200, title);
    }
}
