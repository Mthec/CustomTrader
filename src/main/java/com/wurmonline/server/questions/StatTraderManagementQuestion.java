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
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;

public class StatTraderManagementQuestion extends CustomTraderQuestionExtension {
    private final Creature trader;
    private final String currentTag;
    private final List<String> allTags;
    private static final String NO_TAG = "-";
    private final String[] stats = Stat.getAll();
    private final Stat currentStat;

    public StatTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Karma Trader", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        currentStat = CustomTraderDatabase.getStatFor(trader);
        currentTag = CustomTraderDatabase.getTagFor(trader);
        allTags = CustomTraderDatabase.getAllTags();
        allTags.add(0, NO_TAG);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);
        Creature responder = getResponder();

        if (wasSelected("confirm")) {
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

            int newStatIndex = getIntegerOrDefault("stat", -1);
            String newStat;
            try {
                newStat = stats[newStatIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                newStat = null;
                responder.getCommunicator().sendSafeServerMessage("The trader didn't understand so selected " + currentStat.name + ".");
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
            if (newStat == null && ratio != currentStat.ratio) {
                stat = Stat.create(currentStat.name, ratio);
            } else if (ratio != currentStat.ratio) {
                stat = Stat.create(newStat, ratio);
            } else {
                stat = currentStat;
            }

            if (stat == null) {
                responder.getCommunicator().sendNormalServerMessage("Something went wrong and the trader was not created.");
                return;
            } else if (stat != currentStat) {
                CustomTraderDatabase.setStatFor(trader, stat);
                responder.getCommunicator().sendNormalServerMessage("The trader will now collect " + stat.name + " at a ratio of " + stat.ratio + ":1.");
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
            new CustomTraderItemList(getResponder(), trader, PaymentType.coin).sendQuestion();
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
        List<String> statsList = Arrays.asList(stats);
        String bml = new BMLBuilder(id)
                     .text("Use a 'tag' to use the same inventory contents for multiple custom/currency/karma traders.")
                     .text("Leave blank to keep the inventory unique to this custom trader.")
                     .newLine()
                     .harray(b -> b.label("Name: " + getPrefix()).entry("name", getNameWithoutPrefix(trader.getName()), CustomTraderMod.maxNameLength))
                     .newLine()
                     .text("Stat:")
                     .dropdown("stat", statsList, statsList.indexOf(currentStat.name))
                     .newLine()
                     .text("How many of stat is worth 1i.  e.g. using karma, to buy a 5i item with a ratio of 0.5 it would take 10 karma.")
                     .harray(b -> b.label("Ratio:").entry("ratio", Float.toString(currentStat.ratio), 6))
                     .newLine()
                     .harray(b -> b.label("Tag:").entry("tag", currentTag, CustomTraderMod.maxTagLength))
                     .text(" - or - ")
                     .harray(b -> b.dropdown("tags", Joiner.on(",").join(allTags)).spacer().button("edit", "Edit Tags"))
                     .If(currentTag.isEmpty(),
                             b -> b.text("Setting a tag will remove all items and delete every item on the stock list."),
                             b -> b.text("Changing tags will delete all currently held items."))
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

        getResponder().getCommunicator().sendBml(400, 350, true, true, bml, 200, 200, 200, title);
    }
}
