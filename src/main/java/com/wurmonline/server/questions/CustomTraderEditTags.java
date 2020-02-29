package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomTraderEditTags extends CustomTraderQuestionExtension {
    private final List<String> allTags;

    CustomTraderEditTags(Creature responder) {
        super(responder, "Edit Tags", "Select a tag to remove:", MANAGETRADER, -1);
        allTags = CustomTraderDatabase.getAllTags();
        allTags.remove("");
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        boolean renameTags = wasSelected("rename");
        boolean removeTags = wasSelected("remove");

        if (renameTags || removeTags) {
            List<String> selected = new ArrayList<>();

            for (String item : properties.stringPropertyNames()) {
                if (item.startsWith("s")) {
                    try {
                        if (wasSelected(item)) {
                            int i = Integer.parseInt(item.substring(1));

                            try {
                                selected.add(allTags.get(i));
                            } catch (ArrayIndexOutOfBoundsException e) {
                                logger.warning("Tags out of sync.  Stopping.");
                                getResponder().getCommunicator().sendNormalServerMessage("Something went wrong.  No tags were affected.");
                                return;
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (renameTags) {
                // Properties loses ordering.
                selected.sort(Comparator.comparing(String::toLowerCase));
                new CustomTraderRenameTags(getResponder(), selected).sendQuestion();
            } else {
                int removed = 0;
                for (String tag : selected) {
                    try {
                        CustomTraderDatabase.deleteTag(tag);
                        ++removed;
                    } catch (CustomTraderDatabase.FailedToUpdateTagException e) {
                        logger.warning("Error when attempting to remove tag.");
                        e.printStackTrace();
                        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong.  " + (removed == 0 ? "No " : "Only " + removed) + " tag" + (removed == 1 ? " was " : "s were") + "removed.");
                        break;


                    }
                }

                getResponder().getCommunicator().sendNormalServerMessage(selected.size() + " tag" + (selected.size() == 1 ? " was " : "s were ") + "removed.");
            }
        }
    }

    @Override
    public void sendQuestion() {
        AtomicInteger rowNumber = new AtomicInteger(0);
        String bml = new BMLBuilder(id)
                             .table(new String[] { "Tag", "Select" }, allTags,
                                     (tag, b) -> b.label(tag).checkbox("s" + rowNumber.getAndIncrement(), "", false))
                             .If(allTags.size() == 0, b -> b.label("No tags set."))
                             .newLine()
                             .text("Custom traders with removed tags will be reverted to unique inventory and tag inventory will be deleted.  This cannot be undone.")
                             .newLine()
                             .harray(b -> b.button("rename", "Rename").spacer()
                                           .button("remove", "Remove").spacer()
                                           .button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(300, 400, true, true, bml, 200, 200, 200, title);
    }
}
