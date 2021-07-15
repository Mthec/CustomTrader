package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemTemplate;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Properties;

public class AdvancedCurrencyQuestion extends CustomTraderQuestionExtension {
    private static final DecimalFormat df = new DecimalFormat("0");
    private static final float badQL = -123f;
    private final Creature trader;
    private final ItemTemplate template;
    private final EligibleMaterials materials;
    private final Details details;
    private final float exactQL;
    private final float minQL;

    AdvancedCurrencyQuestion(Creature responder, Creature trader, @NotNull ItemTemplate template, @NotNull Currency currentCurrency) {
        super(responder, "Set Currency (Advanced)", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        this.template = template;
        materials = new EligibleMaterials(template);
        int materialIndex;
        if (currentCurrency.material == -1) {
            materialIndex = 0;
        } else {
            materialIndex = materials.getIndexOf(currentCurrency.material);
            if (materialIndex == 0 && materials.getMaterial(0) != currentCurrency.material) {
                materialIndex = materials.getIndexOf(template.getMaterial()) + 1;
            } else {
                materialIndex += 1;
            }
        }
        details = new Details(0f, materialIndex, currentCurrency.rarity, 0, currentCurrency.onlyFullWeight ? template.getWeightGrams() : 0, currentCurrency.material);
        exactQL = currentCurrency.exactQL;
        minQL = currentCurrency.minQL;
    }

    private AdvancedCurrencyQuestion(Creature responder, Creature trader, ItemTemplate template, Details details, float exactQL, float minQL) {
        super(responder, "Set Currency (Advanced)", "", MANAGETRADER, trader.getWurmId());
        this.trader = trader;
        this.template = template;
        materials = new EligibleMaterials(template);
        this.details = details;
        this.exactQL = exactQL;
        this.minQL = minQL;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();
        
        if (wasSelected("confirm")) {
            int materialIndex = details.materialIndex;
            byte rarity = details.rarity;
            int weight = details.weight;
            boolean reshowStage = false;

            float newExactQL = getQL(answers.getProperty("exact_ql"), "Exact", exactQL);
            if (newExactQL == badQL) {
                newExactQL = exactQL;
                reshowStage = true;
            }

            float newMinQL = getQL(answers.getProperty("min_ql"), "Minimum", minQL);
            if (newMinQL == badQL) {
                newMinQL = minQL;
                reshowStage = true;
            }

            int newMaterialIndex = getIntegerOrDefault("mat", materialIndex);
            byte newMaterial = -1;

            if (newMaterialIndex != materialIndex) {
                if (newMaterialIndex != 0) {
                    try {
                        newMaterial = materials.getMaterial(newMaterialIndex - 1);
                        materialIndex = newMaterialIndex;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}
                }
            } else if (materialIndex != 0) {
                newMaterial = details.aux;
            }

            String newRarity = answers.getProperty("rarity");
            if (newRarity != null) {
                switch (newRarity) {
                    case "-1":
                        rarity = -1;
                        break;
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

            String weightString = answers.getProperty("weight");

            if (weightString == null) {
                responder.getCommunicator().sendNormalServerMessage("Weight was invalid.");
                reshowStage = true;
            } else if (weightString.equals("true")) {
                weight = template.getWeightGrams();
            } else {
                weight = -1;
            }

            if (!reshowStage) {
                CustomTraderDatabase.setCurrencyFor(trader, new Currency(template, newMinQL, newExactQL, newMaterial, rarity, weight == template.getWeightGrams()));
                return;
            }

            new AdvancedCurrencyQuestion(responder, trader, template, new Details(0f, materialIndex, rarity, 0, weight, (byte)0), exactQL, minQL).sendQuestion();
        } else if (wasSelected("back")) {
            new CurrencyTraderManagementQuestion(responder, trader).sendQuestion();
        }
    }

    private float getQL(@Nullable String qlString, String prefix, float current) {
        Creature responder = getResponder();
        try {
            if (qlString == null) {
                responder.getCommunicator().sendNormalServerMessage("No " + prefix + " Quality level received.");
            } else if (qlString.isEmpty()) {
                return -1f;
            } else {
                float newQL = Float.parseFloat(qlString);
                if (newQL != current) {
                    if (newQL <= 0) {
                        responder.getCommunicator().sendNormalServerMessage(prefix + " Quality level must be greater than 0.");
                    } else if (newQL > 100) {
                        responder.getCommunicator().sendNormalServerMessage(prefix + " Quality level cannot be greater than 100.");
                    } else {
                        return newQL;
                    }
                } else {
                    return newQL;
                }
            }
        } catch (NumberFormatException e) {
            responder.getCommunicator().sendNormalServerMessage(prefix + " Quality level was invalid.");
        }

        return badQL;
    }

    @Override
    public void sendQuestion() {
        String bml = new BMLBuilder(id)
                             .text("Set the details for the " + template.getName() + ":")
                             .newLine()
                             .harray(b -> b.label("Material").spacer().dropdown("mat", "any," + materials.getOptions(), details.materialIndex))
                             .newLine()
                             .harray(b -> b.label("Exact Quality Level").spacer().entry("exact_ql", df.format(exactQL), 4))
                             .text("- or -")
                             .harray(b -> b.label("Minimum Quality Level").spacer().entry("min_ql", df.format(minQL), 4))
                             .newLine()
                             .harray(b -> b.spacer()
                                           .radio("rarity", "0", "None", details.rarity == 0).spacer()
                                           .radio("rarity", "1", "Rare", details.rarity == 1).spacer()
                                           .radio("rarity", "2", "Supreme", details.rarity == 2).spacer()
                                           .radio("rarity", "3", "Fantastic", details.rarity == 3).spacer())
                             .newLine()
                             .harray(b -> b.checkbox("weight", "Require full weight?", details.weight == template.getWeightGrams()))
                             .newLine()
                             .harray(b -> b.button("back", "Back").spacer().button("confirm", "Confirm").spacer()
                                           .button("cancel", "Cancel"))
                             .build();

        getResponder().getCommunicator().sendBml(300, 400, true, true, bml, 200, 200, 200, title);
    }
}
