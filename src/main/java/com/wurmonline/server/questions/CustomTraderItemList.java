package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomTraderItemList extends CustomTraderQuestionExtension {
    private final Creature trader;
    private final StockInfo[] stock;

    private static final int day = 24;
    private static final int week = 168;
    private static final int month = 672;

    CustomTraderItemList(Creature responder, Creature trader) {
        super(responder, "Item List", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        stock = CustomTraderDatabase.getStockFor(trader);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        if (wasSelected("cancel"))
            return;

        if (wasSelected("add"))
            new CustomTraderItemsConfigurationQuestion(getResponder(), trader).sendQuestion();
        else if (wasSelected("confirm")) {
            int removed = 0;

            for (String item : properties.stringPropertyNames()) {
                if (item.startsWith("r")) {
                    try {
                        if (wasSelected(item)) {
                            int i = Integer.parseInt(item.substring(1));

                            try {
                                CustomTraderDatabase.removeStockItemFrom(trader, stock[i]);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                logger.warning("Stock removal out of sync.  Stopping.");
                                sendErrorToResponder(removed);
                                break;
                            } catch (CustomTraderDatabase.StockUpdateException e) {
                                logger.warning("Error when attempting to remove item from list.");
                                e.printStackTrace();
                                sendErrorToResponder(removed);
                                break;
                            }

                            ++removed;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            getResponder().getCommunicator().sendNormalServerMessage(removed + " stock item" + (removed == 1 ? " was " : "s were ") + "removed.");
        }
    }

    private void sendErrorToResponder(int removed) {
        getResponder().getCommunicator().sendNormalServerMessage("Something went wrong.  " + (removed == 0 ? "No " : "Only " + removed) + " stock item" + (removed == 1 ? " was " : "s were") + "removed.");
    }

    @Override
    public void sendQuestion() {
        String tag = CustomTraderDatabase.getTagFor(trader);
        AtomicInteger rowNumber = new AtomicInteger(0);
        DecimalFormat df = new DecimalFormat("#.##");
        String bml = new BMLBuilder(id)
                             .If(tag.isEmpty(),
                                  b -> b.text(trader.getName() + "'s item list."),
                                  b -> b.text("Item list for traders with tag '" + tag + "'."))
                             .table(new String[] { "Item", "QL", "Price", "Weight", "Enchants", "Max.", "Rate", "Interval", "Remove?" }, Arrays.asList(stock),
                                     (item, b) -> {
                                     ItemTemplate template;
                                     try {
                                         template = ItemTemplateFactory.getInstance().getTemplate(item.item.templateId);
                                     } catch (NoSuchTemplateException e) {
                                         logger.warning("Template (" + item.item.templateId + ") not found, ignoring.");
                                         e.printStackTrace();
                                         return b;
                                     }
                                     StringBuilder sb = new StringBuilder();
                                     MaterialUtilities.appendNameWithMaterialSuffix(sb, template.getName(), item.item.material);

                                     int rowNum = rowNumber.getAndIncrement();
                                     return b.label(sb.toString())
                                             .label(df.format(item.item.ql))
                                             .label(new Change(item.item.price).getChangeShortString())
                                             .label(WeightHelper.toString(item.item.weight))
                                             .label(String.valueOf(item.item.enchantments.length))
                                             .label(String.valueOf(item.maxNum))
                                             .label((item.restockInterval == 0) ? "N/A" : String.valueOf(item.restockRate))
                                             .label(timeFormat(item.restockInterval))
                                             .checkbox("r" + rowNum, "", false);
                             })
                             .newLine()
                             .harray(b -> b.button("confirm", "Confirm").spacer()
                                                  .button("add", "Add New").spacer()
                                                  .button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(400, 400, true, true, bml, 200, 200, 200, title);
    }

    private String timeFormat(int hours) {
        // Days - Weeks - Months
        if (hours == 0)
            return "N/A";
        else if (hours < day)
            return hours + "h";
        else if (hours < week)
            if (hours % day == 0)
                return hours / day + "d";
            else
                return "~" + hours / day + "d";
        else if (hours < month)
            if (hours % week == 0)
                return hours / week + "w";
            else
                return "~" + hours / week + "w";
        else
            if (hours % month == 0)
                return hours / month + "m";
            else
                return "~" + hours / month + "m";
    }
}
