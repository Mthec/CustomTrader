package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomTraderItemsConfigurationQuestion extends CustomTraderQuestionExtension {
    private final Creature trader;
    private ItemDefinitionStage stage = ItemDefinitionStage.TEMPLATE;

    // Item Settings
    private Template template;
    private Details details;
    private Enchantments enchantments;
    private Restocking restocking;

    private EligibleMaterials materials;

    enum ItemDefinitionStage {
        TEMPLATE,
        DETAILS,
        ENCHANTMENTS,
        ADD_ENCHANTMENT,
        RESTOCKING
    }

    public CustomTraderItemsConfigurationQuestion(Creature responder, Creature trader) {
        super(responder, "Modify Item List", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        EligibleTemplates.init();
        template = Template._default();
        materials = new EligibleMaterials(template.itemTemplate);
        details = Details._default(materials.getIndexOf(template.itemTemplate.getMaterial()));
        enchantments = new Enchantments();
        restocking = Restocking._default();
    }

    public CustomTraderItemsConfigurationQuestion(Creature responder, Creature trader, ItemDefinitionStage stage, Template template, Details details, Enchantments enchantments, Restocking restocking) {
        this(responder, trader);
        this.stage = stage;
        this.template = template;
        this.details = details;
        this.enchantments = enchantments;
        this.restocking = restocking;
        if (template.itemTemplate != null)
            materials = new EligibleMaterials(template.itemTemplate);
    }

    @Override
    public void answer(Properties properties) {
        setAnswer(properties);

        if (wasSelected("cancel"))
            return;

        Creature responder = getResponder();
        boolean reshowStage = false;

        switch (stage) {
            case TEMPLATE:
                if (wasSelected("do_filter")) {
                    String filter = getStringOrDefault("filter", "");
                    template = new Template(0, filter);
                } else {
                    int newTemplateIndex = getIntegerOrDefault("template", template.templateIndex);
                    if (newTemplateIndex != template.templateIndex) {
                        try {
                            template = new Template(template, newTemplateIndex);
                        } catch (ArrayIndexOutOfBoundsException ignored) {}
                    }
                }

                // TODO - How can I break the link between template > material > details?
                if (template.itemTemplate != null) {
                    materials = new EligibleMaterials(template.itemTemplate);
                    details = new Details(details.ql, materials.getIndexOf(template.itemTemplate.getMaterial()), details.rarity, details.price);
                } else
                    reshowStage = true;
                break;
            case DETAILS:
                float ql = details.ql;
                int materialIndex = details.materialIndex;
                byte rarity = details.rarity;
                int price = details.price;

                try {
                    String qlString = properties.getProperty("ql");
                    if (qlString == null) {
                        responder.getCommunicator().sendNormalServerMessage("No Quality level received.");
                        reshowStage = true;
                    } else if (qlString.isEmpty()) {
                        responder.getCommunicator().sendNormalServerMessage("Quality level cannot be empty.");
                        reshowStage = true;
                    } else {
                        float newQL = Float.parseFloat(qlString);
                        if (newQL != details.ql) {
                            if (newQL <= 0) {
                                responder.getCommunicator().sendNormalServerMessage("Quality level must be greater than 0.");
                                reshowStage = true;
                            } else if (newQL > 100) {
                                responder.getCommunicator().sendNormalServerMessage("Quality level cannot be greater than 100.");
                                reshowStage = true;
                            } else {
                                ql = newQL;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    responder.getCommunicator().sendNormalServerMessage("Quality level was invalid.");
                    reshowStage = true;
                }

                int newMaterialIndex = getIntegerOrDefault("mat", materialIndex);

                if (newMaterialIndex != materialIndex) {
                    try {
                        materials.getMaterial(newMaterialIndex);
                        materialIndex = newMaterialIndex;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}
                }

                String newRarity = properties.getProperty("rarity");
                if (newRarity != null) {
                    switch (newRarity) {
                        case "0":
                            rarity = 0;
                            break;
                        case "1":
                            rarity = 1;
                            break;
                        case "2":
                            rarity = 2;
                            break;
                        case "3":
                            rarity = 3;
                            break;
                    }
                }

                try {
                    String priceString = properties.getProperty("price");

                    if (priceString == null) {
                        responder.getCommunicator().sendNormalServerMessage("Price was invalid.");
                        reshowStage = true;
                    } else if (priceString.isEmpty()) {
                        responder.getCommunicator().sendNormalServerMessage("Price was empty.");
                        reshowStage = true;
                    } else {
                        int newPrice = Integer.parseInt(priceString);
                        if (newPrice != price) {
                            if (newPrice <= 0) {
                                responder.getCommunicator().sendNormalServerMessage("Price must be greater than 0 irons.");
                                reshowStage = true;
                            } else {
                                price = newPrice;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    responder.getCommunicator().sendNormalServerMessage("Price was invalid.");
                    reshowStage = true;
                }

                details = new Details(ql, materialIndex, rarity, price);
                break;
            case ENCHANTMENTS:
                int i = 0;
                List<Spell> toRemove = new ArrayList<>();
                Map<Spell, Float> toChange = new HashMap<>();

                for (Enchantment enchantment : enchantments) {
                    if (wasSelected("r" + i)) {
                        toRemove.add(enchantment.spell);
                    } else {
                        float power = getFloatOrDefault("p" + i, 1);
                        toChange.put(enchantment.spell, power);
                    }
                    ++i;
                }

                for (Spell spell : toRemove)
                    enchantments.remove(spell);

                for (Map.Entry<Spell, Float> entry : toChange.entrySet())
                    enchantments.setPower(entry.getKey(), entry.getValue());

                break;
            case ADD_ENCHANTMENT:
                if (wasSelected("back")) {
                    properties.setProperty("ENCHANTMENTS", "true");
                } else {
                    int enchant = getIntegerOrDefault("enchant", -1);
                    float power = 0;

                    try {
                        String powerString = properties.getProperty("power");
                        if (powerString == null) {
                            responder.getCommunicator().sendNormalServerMessage("No power level received.");
                            reshowStage = true;
                        } else if (powerString.isEmpty()) {
                            responder.getCommunicator().sendNormalServerMessage("Power level cannot be empty.");
                            reshowStage = true;
                        } else {
                            float newPower = Float.parseFloat(powerString);
                            try {
                                Spell spell = Enchantments.allEnchantments[enchant];
                                if (newPower != enchantments.getPower(spell)) {
                                    if (newPower <= 0) {
                                        responder.getCommunicator().sendNormalServerMessage("Power level must be greater than 0.");
                                        reshowStage = true;
                                    } else if (newPower > 999) {
                                        responder.getCommunicator().sendNormalServerMessage("Power level cannot be greater than 999.");
                                        reshowStage = true;
                                    } else {
                                        power = newPower;
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignored) {}
                        }
                    } catch (NumberFormatException e) {
                        responder.getCommunicator().sendNormalServerMessage("Power level was invalid.");
                        reshowStage = true;
                    }

                    if (enchant != -1 && power > 0)
                        enchantments.setPower(Enchantments.allEnchantments[enchant], power);
                }
                break;
            case RESTOCKING:
                int restockRate = getPositiveIntegerOrDefault("rate", restocking.restockRate);
                int restockInterval = getPositiveIntegerOrDefault("interval", restocking.restockInterval);

                int maxStock = getPositiveIntegerOrDefault("max stock", restocking.maxStock);

                if (maxStock == 0) {
                    responder.getCommunicator().sendNormalServerMessage("Max stock cannot be 0.");
                    reshowStage = true;
                    maxStock = restocking.maxStock;
                }

                restocking = new Restocking(maxStock, restockRate, restockInterval);
                break;
        }

        if (!reshowStage) {
            if (wasSelected("TEMPLATE")) {
                stage = ItemDefinitionStage.TEMPLATE;
            } else if (wasSelected("DETAILS")) {
                stage = ItemDefinitionStage.DETAILS;
            } else if (wasSelected("RESTOCKING")) {
                stage = ItemDefinitionStage.RESTOCKING;
            } else if (wasSelected("ENCHANTMENTS")) {
                stage = ItemDefinitionStage.ENCHANTMENTS;
            } else if (wasSelected("ADD_ENCHANTMENT")) {
                stage = ItemDefinitionStage.ADD_ENCHANTMENT;
            } else if (wasSelected("END")) {
                List<Enchantment> enchants = enchantments.toList();

                try {
                    CustomTraderDatabase.addStockItemTo(trader, template.itemTemplate.getTemplateId(),
                            details.ql, details.price, materials.getMaterial(details.materialIndex), details.rarity,
                            enchants.toArray(new Enchantment[0]),
                            restocking.maxStock, restocking.restockRate, restocking.restockInterval);
                    responder.getCommunicator().sendNormalServerMessage(trader.getName() + " adds the new stock to their list.");
                    return;
                } catch (CustomTraderDatabase.StockUpdateException e) {
                    responder.getCommunicator().sendNormalServerMessage(trader.getName() + "'s eyes glaze over and they seem to forget what they were doing.");
                }
            }
        }

        new CustomTraderItemsConfigurationQuestion(responder, trader, stage, template, details, enchantments, restocking)
                .sendQuestion();
    }

    private int getPositiveIntegerOrDefault(String id, int _default) {
        String f = getAnswer().getProperty(id);
        if (f != null) {
            if (f.isEmpty()) {
                getResponder().getCommunicator().sendNormalServerMessage(StringUtilities.raiseFirstLetter(id) + " was empty.");
                return _default;
            }

            try {
                int value = Integer.parseInt(f);
                if (value < 0) {
                    getResponder().getCommunicator().sendNormalServerMessage(StringUtilities.raiseFirstLetter(id) + " must not be a negative number.");
                    return _default;
                }

                return value;
            } catch (NumberFormatException ignored) {}
        }

        getResponder().getCommunicator().sendNormalServerMessage(StringUtilities.raiseFirstLetter(id) + " was invalid.");
        return _default;
    }

    @Override
    public void sendQuestion() {
        BML bml = new BMLBuilder(id);
        switch (stage) {
            case TEMPLATE:
                bml = bml.text("Select a template:")
                    .newLine()
                    .text("Filter available templates:")
                    .text("* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.")
                    .newLine()
                    .harray(b -> b.entry("filter", template.filter, 10).spacer()
                              .button("do_filter", "Apply"))
                    .newLine()
                    .harray(b -> b.dropdown("template", template.getOptions(), template.templateIndex))
                    .newLine()
                    .harray(b -> b.If(template.itemTemplate != null, b2 -> b2.button("DETAILS", "Next").spacer()).button("cancel", "Cancel"));
                break;
            case DETAILS:
                bml = bml.text("Set the details for the " + template.itemTemplate.getName() + ":")
                        .newLine()
                        .harray(b -> b.label("Quality Level").spacer().entry("ql", Float.toString(details.ql), 4).spacer())
                        .newLine()
                        .harray(b -> b.label("Material").spacer().dropdown("mat", materials.getOptions(), details.materialIndex))
                        .newLine()
                        .harray(b -> b.spacer()
                                .radio("rarity", "0", "None", details.rarity == 0).spacer()
                                .radio("rarity", "1", "Rare", details.rarity == 1).spacer()
                                .radio("rarity", "2", "Supreme", details.rarity == 2).spacer()
                                .radio("rarity", "3", "Fantastic", details.rarity == 3).spacer()
                        )
                        .newLine()
                        .harray(b -> b.label("Price").spacer().entry("price", Long.toString(details.price), 10).spacer().text("irons"))
                        .newLine()
                        .harray(b -> b.button("TEMPLATE", "Back").spacer().button("RESTOCKING", "Next").spacer()
                                             .button("ENCHANTMENTS", "Enchantments").spacer().button("cancel", "Cancel"));
                break;
            case ENCHANTMENTS:
                AtomicInteger i = new AtomicInteger(0);
                bml = bml.text("Item Enchantments")
                              .table(new String[] { "Name", "Power", "Remove?" }, enchantments.toList(),
                                      (enchantment, b) -> b.label(enchantment.name)
                                      .entry("p" + i, Float.toString(enchantment.power), 4)
                                      .checkbox("r" + i.getAndIncrement(), ""))
                    .newLine()
                    .harray(b -> b.button("RESTOCKING", "Next").spacer().button("ENCHANTMENTS", "Apply").spacer().button("ADD_ENCHANTMENT", "Add Enchantment").spacer().button("cancel", "Cancel"));
                break;
            case ADD_ENCHANTMENT:
                bml = bml.text("Select an enchantment:")
                              .newLine()
                              .harray(b -> b.dropdown("enchant", Enchantments.allEnchantmentsString))
                              .newLine()
                              .harray(b -> b.label("Power").spacer().entry("power", "0", 4))
                              .newLine()
                              .harray(b -> b.button("ENCHANTMENTS", "Add").spacer().button("back", "Back"));
                break;
            case RESTOCKING:
                bml = bml.text("Set the restock schedule for the " + ItemFactory.generateName(template.itemTemplate, materials.getMaterial(details.materialIndex)) + ":")
                        .newLine()
                        .label("Rate:").entry("rate", Integer.toString(restocking.restockRate), 4).text("How many items to replace each interval.  0 means restock all.  Ignored if Interval is 0.").italic()
                        .label("Interval (Hours):").entry("interval", Integer.toString(restocking.restockInterval), 4).text("0 means after every sale.  Day - 24, Week - 168, Month (28 days) - 672").italic()
                        .label("Stock:").entry("max stock", Integer.toString(restocking.maxStock), 3).text("The maximum (and starting) number of items to stock at one time.").italic()
                        .newLine()
                        .harray(b -> b.button("DETAILS", "Back").spacer().button("END", "Finish").spacer().button("cancel", "Cancel"));
                break;
        }

        getResponder().getCommunicator().sendBml(300, 400, true, true, bml.build(), 200, 200, 200, title);
    }
}
