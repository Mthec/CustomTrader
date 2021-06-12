package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;

public abstract class PlaceOrManageTraderQuestion extends CustomTraderQuestionExtension {
    private static final Random r = new Random();
    protected static final String NO_TAG = "-";
    protected final List<String> allTags;

    PlaceOrManageTraderQuestion(Creature responder, String title, long target) {
        super(responder, title, "", MANAGETRADER, target);
        allTags = CustomTraderDatabase.getAllTags();
        allTags.add(0, NO_TAG);
    }

    protected byte getGender() {
        byte sex = 0;
        if (wasAnswered("gender", "female"))
            sex = 1;

        return sex;
    }

    protected String getName(byte sex) {
        Creature responder = getResponder();
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

        return name;
    }

    protected void checkSaveName(Creature trader) {
        String fullName = getPrefix() + StringUtilities.raiseFirstLetter(getName(trader.getSex()));
        if (!fullName.equals(trader.getName())) {
            try {
                saveCreatureName(trader, fullName);
                trader.refreshVisible();
                getResponder().getCommunicator().sendNormalServerMessage("The trader will now be known as " + trader.getName() + ".");
            } catch (IOException e) {
                logger.warning("Failed to set name (" + fullName + ") for creature (" + trader.getWurmId() + ").");
                getResponder().getCommunicator().sendNormalServerMessage("The trader looks confused, what exactly is a database?");
                e.printStackTrace();
            }
        }
    }

    protected String getTag() {
        int dropdown = getIntegerOrDefault("tags", 0);
        String tag;

        if (dropdown != 0) {
            tag = allTags.get(dropdown);
            if (tag.equals(NO_TAG))
                tag = "";
        } else {
            tag = getStringProp("tag");
            if (tag.length() > CustomTraderMod.maxTagLength) {
                getResponder().getCommunicator().sendAlertServerMessage("The tag was too long, so it was cut short.");
                tag = tag.substring(0, CustomTraderMod.maxTagLength);
            }
        }

        return tag;
    }

    protected void checkSaveTag(Creature trader, String currentTag) {
        String tag = getTag();

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
    }

    protected boolean locationIsValid(Creature responder, @Nullable VolaTile tile) {
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

    protected void checkStockOptions(Creature trader) {
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
    }

    protected void tryDismiss(Creature trader, String traderType) {
        Creature responder = getResponder();
        if (!trader.isTrading()) {
            Server.getInstance().broadCastAction(trader.getName() + " grunts, packs " + trader.getHisHerItsString() + " things and is off.", trader, 5);
            responder.getCommunicator().sendNormalServerMessage("You dismiss " + trader.getName() + " from " + trader.getHisHerItsString() + " post.");
            logger.info(responder.getName() + " dismisses " + traderType + " trader " + trader.getName() + " with WurmID: " + target);

            try {
                CustomTraderDatabase.deleteAllStockFor(trader);
            } catch (CustomTraderDatabase.StockUpdateException e) {
                logger.warning("Failed to delete stock when dismissing " + traderType + " trader.  Some entries may still remain.");
                e.printStackTrace();
            }
            trader.destroy();
        } else {
            responder.getCommunicator().sendNormalServerMessage(trader.getName() + " is trading. Try later.");
        }
    }

    // sendQuestion

    protected BML middleBML(BML bml, String namePlaceholder) {
        return bml
                .text("Use a 'tag' to use the same inventory contents for multiple custom/currency/stat traders.")
                .text("Leave blank to keep the inventory unique to this custom trader.")
                .newLine()
                .harray(b -> b.label("Name: " + getPrefix()).entry("name", namePlaceholder, CustomTraderMod.maxNameLength))
                .newLine();
    }

    private BML addTagSelector(BML bml, String currentTag) {
        return bml
                .harray(b -> b.label("Tag:").entry("tag", currentTag, CustomTraderMod.maxTagLength))
                .text(" - or - ")
                .harray(b -> b.dropdown("tags", Joiner.on(",").join(allTags)).spacer().button("edit", "Edit Tags"));
    }

    protected String endBML(BML bml) {
        boolean gender = r.nextBoolean();
        return addTagSelector(bml, "")
                  .newLine()
                  .text("Gender:")
                  .radio("gender", "male", "Male", gender)
                  .radio("gender", "female", "Female", !gender)
                  .newLine()
                  .harray(b -> b.button("Send"))
                  .build();
    }

    protected String endBML(BML bml, String currentTag, Creature trader) {
        return addTagSelector(bml, currentTag)
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
    }
}
