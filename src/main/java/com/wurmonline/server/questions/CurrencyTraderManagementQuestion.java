package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.Properties;

public class CurrencyTraderManagementQuestion extends PlaceOrManageTraderQuestion {
    private final Creature trader;
    private final Currency currency;
    private final String currentTag;
    private Template template;

    public CurrencyTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Currency Trader", trader);
        this.trader = trader;
        currentTag = CustomTraderDatabase.getTagFor(trader);
        EligibleTemplates.init();
        Currency curr = CustomTraderDatabase.getCurrencyFor(trader);
        if (curr == null) {
            template = Template.getForTemplateId(0);
            if (template.itemTemplate != null) {
                currency = new Currency(template.itemTemplate);
            } else {
                // Hiding the error is not good, but handling it would be complicated.  Shouldn't ever happen.
                currency = new Currency(ItemTemplateFactory.getInstance().getTemplates()[ItemList.acorn]);
            }
        } else {
            currency = curr;
            template = Template.getForTemplateId(currency.templateId);
        }
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        if (wasSelected("do_filter")) {
            String filter = getStringOrDefault("filter", "");
            template = new Template(0, filter);

            CurrencyTraderManagementQuestion question = new CurrencyTraderManagementQuestion(responder, trader);
            question.template = new Template(0, filter);
            question.sendQuestion();
        } else if (wasSelected("confirm")) {
            checkSaveName(trader);
            parseCurrency();

            if (template.itemTemplate != null && template.itemTemplate.getTemplateId() != currency.templateId) {
                if (currency.material == -1 || new EligibleMaterials(template.itemTemplate).isEligible(currency.material)) {
                    CustomTraderDatabase.setCurrencyFor(trader, template.itemTemplate, currency);
                    getResponder().getCommunicator().sendNormalServerMessage(trader.getName() + "'s currency was set to " + template.itemTemplate.getPlural() + ".");
                } else {
                    CustomTraderDatabase.setCurrencyFor(trader, template.itemTemplate, template.itemTemplate.getMaterial(), currency);
                    getResponder().getCommunicator().sendNormalServerMessage(trader.getName() + "'s currency was set to " + template.itemTemplate.getPlural() + ", and currency material was changed to " +
                                                                                template.itemTemplate.getMaterial() + ".");
                }
            } else if (template.itemTemplate == null) {
                getResponder().getCommunicator().sendNormalServerMessage("No valid currency was selected, ignoring.");
            }

            checkSaveTag(trader, currentTag);
            checkStockOptions(trader);
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader, PaymentType.currency).sendQuestion();
        } else if (wasSelected("advanced")) {
            parseCurrency();

            ItemTemplate itemTemplate = template.itemTemplate;
            if (itemTemplate != null) {
                new AdvancedCurrencyQuestion(getResponder(), trader, template.itemTemplate, currency).sendQuestion();
            } else {
                getResponder().getCommunicator().sendNormalServerMessage("No valid currency was selected.");
            }
        } else if (wasSelected("dismiss")) {
            tryDismiss(trader, "custom");
        } else {
            checkCustomise(trader);
        }
    }

    private void parseCurrency() {
        int newTemplateIndex = getIntegerOrDefault("template", template.templateIndex);
        if (newTemplateIndex != template.templateIndex) {
            try {
                template = new Template(template, newTemplateIndex);
            } catch (ArrayIndexOutOfBoundsException e) {
                getResponder().getCommunicator().sendNormalServerMessage("Invalid currency selected, " + trader.getName() + "'s currency was not changed.");
            }
        }
    }

    @Override
    public void sendQuestion() {
        BML bml = middleBML(new BMLBuilder(id), getNameWithoutPrefix(trader.getName()))
                             .text("Currency:")
                             .text("Filter available templates:")
                             .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                             .newLine()
                             .harray(b -> b.label("Filter:")
                                           .entry("filter", template.filter, 10).spacer()
                                           .button("do_filter", "Apply"))
                             .text("Select a template and click 'Advanced' to set extra currency settings like material and rarity.")
                             .harray(b -> b.dropdown("template", template.getOptions(), template.templateIndex).spacer()
                                           .button("advanced", "Advanced"))
                             .newLine();

        getResponder().getCommunicator().sendBml(450, 400, true, true, endBML(bml, currentTag, trader), 200, 200, 200, title);
    }
}
