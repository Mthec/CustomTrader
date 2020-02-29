package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomTraderRenameTags extends CustomTraderQuestionExtension {
    private final List<String> toRename;

    CustomTraderRenameTags(Creature responder, List<String> toRename) {
        super(responder, "Rename Tags", "", MANAGETRADER, -1);
        this.toRename = toRename;
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        if (wasSelected("rename")) {
            int renamed = 0;
            for (String item : properties.stringPropertyNames()) {
                if (item.startsWith("t")) {
                    int i = Integer.parseInt(item.substring(1));

                    try {
                        CustomTraderDatabase.renameTag(toRename.get(i), properties.getProperty(item));
                        ++renamed;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        logger.warning("Tags out of sync.  Stopping.");
                        sendErrorToResponder(renamed);
                        return;
                    } catch (CustomTraderDatabase.FailedToUpdateTagException e) {
                        logger.warning("Error when attempting to rename tag.");
                        e.printStackTrace();
                        sendErrorToResponder(renamed);
                        break;
                    }
                }
            }

            getResponder().getCommunicator().sendNormalServerMessage(renamed + " tag" + (renamed == 1 ? " was " : "s were ") + "renamed.");
        }
    }

    private void sendErrorToResponder(int renamed) {
        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong.  " + (renamed == 0 ? "No " : "Only " + renamed) + " tag" + (renamed == 1 ? " was " : "s were") + "renamed.");
    }

    @Override
    public void sendQuestion() {
        AtomicInteger rowNumber = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                             .table(new String[] { "From", "To" }, toRename,
                                     (tag, b) -> b.label(tag).entry("t" + rowNumber.getAndIncrement(), tag, CustomTraderMod.maxTagLength))
                             .newLine()
                             .text("Unchanged tags will be ignored.")
                             .newLine()
                             .harray(b -> b.button("rename", "Rename").spacer()
                                           .button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(300, 400, true, true, bml, 200, 200, 200, title);
    }
}
