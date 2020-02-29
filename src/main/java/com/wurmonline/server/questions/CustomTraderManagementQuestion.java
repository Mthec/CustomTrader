package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.List;
import java.util.Properties;

public class CustomTraderManagementQuestion extends CustomTraderQuestionExtension {
    private final Creature trader;
    private final String currentTag;
    private final List<String> allTags;
    private static final String NO_TAG = "-";

    public CustomTraderManagementQuestion(Creature responder, Creature trader) {
        super(responder, "Manage Custom Trader", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        currentTag = CustomTraderDatabase.getTagFor(trader);
        allTags = CustomTraderDatabase.getAllTags();
        allTags.add(0, NO_TAG);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        if (wasSelected("confirm")) {
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

                getResponder().getCommunicator().sendNormalServerMessage(sb.toString());
            }

            if (wasSelected("empty")) {
                for (Item item : trader.getInventory().getItemsAsArray()) {
                    Items.destroyItem(item.getWurmId());
                }
                getResponder().getCommunicator().sendNormalServerMessage(trader.getName() + " got rid of their stock.");
            }

            if (wasSelected("full")) {
                CustomTraderDatabase.fullyStock(trader);
                getResponder().getCommunicator().sendNormalServerMessage(trader.getName() + " is now fully stocked.");
            }
        } else if (wasSelected("edit")) {
            new CustomTraderEditTags(getResponder()).sendQuestion();
        } else if (wasSelected("list")) {
            new CustomTraderItemList(getResponder(), trader).sendQuestion();
        } else if (wasSelected("dismiss")) {
            Creature responder = getResponder();

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
                     .text("Use a 'tag' to use the same inventory contents for multiple custom traders.")
                     .text("Leave blank to keep the inventory unique to this custom trader.")
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
