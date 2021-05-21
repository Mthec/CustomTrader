package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;

public class CurrencyTraderManagementQuestion extends CustomTraderQuestionExtension {
    private final Creature trader;
    private final int currency;
    private final String currentTag;
    private final List<String> allTags;
    private static final String NO_TAG = "-";
    private Template template;

    public CurrencyTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Currency Trader", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        currentTag = CustomTraderDatabase.getTagFor(trader);
        allTags = CustomTraderDatabase.getAllTags();
        allTags.add(0, NO_TAG);
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
            String name = getStringProp("name");
            if (name != null && !name.isEmpty()) {
                String fullName = getPrefix() + StringUtilities.raiseFirstLetter(name);
                if (QuestionParser.containsIllegalCharacters(name)) {
                    responder.getCommunicator().sendNormalServerMessage("The trader didn't like that name, so they shall remain " + trader.getName() + ".");
                } else if (!fullName.equals(trader.getName())) {
                    try {
                        saveCreatureName(trader, fullName);
                        trader.refreshVisible();
                        responder.getCommunicator().sendNormalServerMessage("The trader will now be known as " + trader.getName() + ".");
                    } catch (IOException e) {
                        logger.warning("Failed to set name (" + fullName + ") for creature (" + trader.getWurmId() + ").");
                        responder.getCommunicator().sendNormalServerMessage("The trader looks confused, what exactly is a database?");
                        e.printStackTrace();
                    }
                }
            }

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

            int dropdown = getIntegerOrDefault("tags", 0);
            String tag;

            if (dropdown != 0) {
                tag = allTags.get(dropdown);
                if (tag.equals(NO_TAG))
                    tag = "";
            } else
                tag = getStringProp("tag");

            if (!tag.equals(currentTag)) {
                StringBuilder sb = new StringBuilder();
                sb.append(trader.getName());

                if (tag.isEmpty())
                    sb.append(" was set to use a unique inventory.");
                else
                    sb.append("'s tag was set to '").append(tag).append("'.");

                try {
                    if (currentTag.isEmpty())
                        CustomTraderDatabase.deleteAllStockFor(trader);
                    CustomTraderDatabase.updateTag(trader, tag);
                } catch (CustomTraderDatabase.StockUpdateException | CustomTraderDatabase.FailedToUpdateTagException e) {
                    sb = new StringBuilder(trader.getName()).append(" looks at the ground and does nothing.");
                }

                responder.getCommunicator().sendNormalServerMessage(sb.toString());
            }

            if (wasSelected("empty")) {
                for (Item item : trader.getInventory().getItemsAsArray()) {
                    Items.destroyItem(item.getWurmId());
                }
                responder.getCommunicator().sendNormalServerMessage(trader.getName() + " got rid of their stock.");
            }

            if (wasSelected("full")) {
                CustomTraderDatabase.fullyStock(trader);
                responder.getCommunicator().sendNormalServerMessage(trader.getName() + " is now fully stocked.");
            }
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader, PaymentType.currency).sendQuestion();
        } else if (wasSelected("dismiss")) {
            if (trader != null) {
                if (!trader.isTrading()) {
                    Server.getInstance().broadCastAction(trader.getName() + " grunts, packs " + trader.getHisHerItsString() + " things and is off.", trader, 5);
                    responder.getCommunicator().sendNormalServerMessage("You dismiss " + trader.getName() + " from " + trader.getHisHerItsString() + " post.");
                    logger.info(responder.getName() + " dismisses custom trader " + trader.getName() + " with WurmID: " + target);

                    try {
                        CustomTraderDatabase.deleteAllStockFor(trader);
                    } catch (CustomTraderDatabase.StockUpdateException e) {
                        logger.warning("Failed to delete stock when dismissing custom trader.  Some entries may still remain.");
                        e.printStackTrace();
                    }
                    trader.destroy();
                } else {
                    responder.getCommunicator().sendNormalServerMessage(trader.getName() + " is trading. Try later.");
                }
            }
        }
    }

    @Override
    public void sendQuestion() {
        String bml = new BMLBuilder(id)
                             .text("Use a 'tag' to use the same inventory contents for multiple custom/currency traders.")
                             .text("Leave blank to keep the inventory unique to this custom trader.")
                             .newLine()
                             .harray(b -> b.label("Name: " + getPrefix()).entry("name", getNameWithoutPrefix(trader.getName()), CustomTraderMod.maxNameLength))
                             .newLine()
                             .harray(b -> b.label("Tag:").entry("tag", currentTag, CustomTraderMod.maxTagLength))
                             .text(" - or - ")
                             .harray(b -> b.dropdown("tags", Joiner.on(",").join(allTags)).spacer().button("edit", "Edit Tags"))
                             .If(currentTag.isEmpty(),
                                     b -> b.text("Setting a tag will remove all items and delete every item on the stock list."),
                                     b -> b.text("Changing tags will delete all currently held items."))
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
                             .checkbox("empty", "Remove all items from inventory?", false)
                             .checkbox("full", "Fully stock all items?", false)
                             .newLine()
                             .harray(b -> b
                                  .button("confirm", "Confirm").spacer()
                                  .button("list", "Items List").spacer()
                                  .button("dismiss", "Dismiss").confirm("Dismiss trader", "Are you sure you wish to dismiss " + trader.getName() + "?").spacer()
                                  .button("cancel", "Cancel").spacer())
                             .build();

        getResponder().getCommunicator().sendBml(450, 400, true, true, bml, 200, 200, 200, title);
    }
}
