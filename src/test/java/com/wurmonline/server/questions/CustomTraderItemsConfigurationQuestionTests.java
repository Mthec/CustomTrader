package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.Spells;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static com.wurmonline.server.questions.CustomTraderItemsConfigurationQuestion.ItemDefinitionStage.*;
import static mod.wurmunlimited.Assert.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CustomTraderItemsConfigurationQuestionTests extends CustomTraderTest {
    private CustomTraderObjectsFactory factory;
    private Creature trader;
    private Player gm;
    private static final int templateIndex = 905;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        factory = new CustomTraderObjectsFactory();
        trader = factory.createNewCustomTrader();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
    }

    private CustomTraderItemsConfigurationQuestion getQuestionAtStage(CustomTraderItemsConfigurationQuestion.ItemDefinitionStage stage) {
        try {
            CustomTraderItemsConfigurationQuestion question = new CustomTraderItemsConfigurationQuestion(gm, trader, PaymentType.coin);
            ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("stage"), stage);

            if (stage != TEMPLATE) {
                Template template = new Template(templateIndex, "");
                assert template.itemTemplate.getName().equals("rake");
                ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("template"), template);
                EligibleMaterials materials = new EligibleMaterials(template.itemTemplate);
                ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("materials"), materials);
                ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("details"), Details._default(materials.getIndexOf(template.itemTemplate.getMaterial()), template.itemTemplate.getWeightGrams()));
            }
            return question;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties getCorrectDetails() {
        Properties properties = new Properties();
        properties.setProperty("ql", "20");
        properties.setProperty("mat", "4");
        properties.setProperty("rarity", "0");
        properties.setProperty("price", "1");
        properties.setProperty("weight", "1.2");
        properties.setProperty("aux", "0");
        return properties;
    }

    private Properties getCorrectRestocking() {
        Properties properties = new Properties();
        properties.setProperty("rate", "0");
        properties.setProperty("interval", "1");
        properties.setProperty("max stock", "1");
        return properties;
    }

    private void assertInfoCorrect(StockInfo info) {
        StockItem item = info.item;
        assertEquals(ItemList.rake, item.templateId);
        assertEquals(20f, item.ql);
        assertEquals(ItemMaterials.MATERIAL_IRON, item.material);
        assertEquals((byte)0, item.rarity);
        assertEquals(1, item.price);
        assertEquals(1200, item.weight);
        assertEquals(0, info.restockRate);
        assertEquals(1, info.restockInterval);
        assertEquals(1, info.maxNum);
    }

    @Test
    void testNothingChangesWithCancelTEMPLATE() {
        Properties answers = new Properties();
        answers.setProperty("template", "1");
        answers.setProperty("cancel", "true");
        getQuestionAtStage(TEMPLATE).answer(answers);

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testNothingChangesWithCancelDETAILS() {
        Properties answers = getCorrectDetails();
        answers.setProperty("cancel", "true");
        getQuestionAtStage(DETAILS).answer(answers);

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testNothingChangesWithCancelRESTOCKING() {
        Properties answers = getCorrectRestocking();
        answers.setProperty("cancel", "true");
        getQuestionAtStage(RESTOCKING).answer(answers);

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testNextScreenWithCorrectTEMPLATE() {
        Properties answers = new Properties();
        answers.setProperty("template", String.valueOf(templateIndex));
        answers.setProperty("DETAILS", "true");
        getQuestionAtStage(TEMPLATE).answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);

        getQuestionAtStage(DETAILS).sendQuestion();
        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
    }

    @Test
    void testNextScreenWithCorrectDETAILS() {
        Properties answers = getCorrectDetails();
        answers.setProperty("RESTOCKING", "true");
        getQuestionAtStage(DETAILS).answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);

        getQuestionAtStage(RESTOCKING).sendQuestion();
        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
    }

    @Test
    void testNextScreenWithCorrectDETAILS_ENCHANTMENTS() {
        Properties answers = getCorrectDetails();
        answers.setProperty("ENCHANTMENTS", "true");
        getQuestionAtStage(DETAILS).answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);

        getQuestionAtStage(ENCHANTMENTS).sendQuestion();
        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
    }

    @Test
    void testNextScreenWithCorrectENCHANTMENTS() {
        Properties answers = getCorrectDetails();
        answers.setProperty("RESTOCKING", "true");
        getQuestionAtStage(ENCHANTMENTS).answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);

        getQuestionAtStage(RESTOCKING).sendQuestion();
        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
    }

    @Test
    void testStockUpdatedWithCorrectRESTOCKING() {
        Properties answers = getCorrectRestocking();
        answers.setProperty("END", "true");
        getQuestionAtStage(RESTOCKING).answer(answers);

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testStockUpdatedWithCompleteOrder() {
        CustomTraderItemsConfigurationQuestion question = new CustomTraderItemsConfigurationQuestion(gm, trader, PaymentType.coin);
        question.sendQuestion();
        Properties answers = new Properties();
        answers.setProperty("template", String.valueOf(templateIndex));
        answers.setProperty("DETAILS", "true");
        question.answer(answers);

        answers = getCorrectDetails();
        answers.setProperty("RESTOCKING", "true");
        question.answer(answers);

        answers = getCorrectRestocking();
        answers.setProperty("END", "true");
        question.answer(answers);

        assertEquals(3, factory.getCommunicator(gm).getBml().length);

        StockInfo[] items = CustomTraderDatabase.getStockFor(trader);
        assertEquals(1, items.length);

        assertInfoCorrect(items[0]);
    }

    @Test
    void testTEMPLATEInvalidResetsSilently() {
        Properties answers = new Properties();
        answers.setProperty("template", "abc");
        answers.setProperty("DETAILS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(TEMPLATE);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlNotEqual());
    }

    @Test
    void testTEMPLATENegativeResetsSilently() {
        Properties answers = new Properties();
        answers.setProperty("template", "-1");
        answers.setProperty("DETAILS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(TEMPLATE);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlNotEqual());
    }

    @Test
    void testTEMPLATEWithNoFilterResults() {
        Properties answers = new Properties();
        answers.setProperty("filter", "zyx");
        answers.setProperty("do_filter", "true");
        assertDoesNotThrow(() -> getQuestionAtStage(TEMPLATE).answer(answers));
        assertFalse(factory.getCommunicator(gm).lastBmlContent.contains("DETAILS"));
    }

    @Test
    void testTEMPLATEWithNoFilterResultsReAddsNextButtonIfFilterChanged() {
        Properties answers = new Properties();
        answers.setProperty("filter", "zyx");
        answers.setProperty("do_filter", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(TEMPLATE);
        assertDoesNotThrow(() -> question.answer(answers));
        assertFalse(factory.getCommunicator(gm).lastBmlContent.contains("DETAILS"));

        Properties answers2 = new Properties();
        answers2.setProperty("filter", "");
        answers2.setProperty("do_filter", "true");
        assertDoesNotThrow(() -> getQuestionAtStage(TEMPLATE).answer(answers2));

        assertTrue(factory.getCommunicator(gm).lastBmlContent.contains("DETAILS"));
    }

    @Test
    void testDETAILSNegativeQLReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("ql", "-1");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Quality level must be greater"));
    }

    @Test
    void testDETAILSOver100QLReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("ql", "101");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Quality level cannot be greater"));
    }

    @Test
    void testDETAILSInvalidQLReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("ql", "abc");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Quality level was invalid"));
    }

    @Test
    void testDETAILSMissingQLReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.remove("ql");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        assertDoesNotThrow(() -> question.answer(answers));

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("No Quality level received"));
    }

    @Test
    void testDETAILSEmptyQLReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("ql", "");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Quality level cannot be empty"));
    }

    @Test
    void testDETAILSInvalidMaterialResetsSilently() throws NoSuchFieldException, IllegalAccessException, NoSuchTemplateException {
        Properties answers = getCorrectDetails();
        answers.setProperty("mat", "100");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        EligibleMaterials materials = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("materials"));
        int materialIndex = ((Details)ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("details"))).materialIndex;
        assertEquals(materials.getIndexOf(ItemMaterials.MATERIAL_IRON), materialIndex);
        assertEquals(ItemTemplateFactory.getInstance().getTemplate(ItemList.rake).getMaterial(), materials.getMaterial(materialIndex));
    }

    @Test
    void testDETAILSInvalidRarityResetsSilently() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = getCorrectDetails();
        answers.setProperty("rarity", "100");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.answer(answers);

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals((byte)0, ((Details)ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("details"))).rarity);
    }

    @Test
    void testDETAILSZeroPriceReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("price", "0");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Price must be greater"));
    }

    @Test
    void testDETAILSInvalidPriceReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("price", "abc");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Price was invalid"));
    }

    @Test
    void testDETAILSMissingPriceReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.remove("price");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        assertDoesNotThrow(() -> question.answer(answers));

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Price was invalid"));
    }

    @Test
    void testDETAILSEmptyPriceReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("price", "");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Price was empty"));
    }

    @Test
    void testDETAILSZeroWeightReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("weight", "0");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Weight must be greater"));
    }

    @Test
    void testDETAILSInvalidWeightReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("weight", "abc");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Weight was invalid"));
    }

    @Test
    void testDETAILSMissingWeightReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.remove("weight");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        assertDoesNotThrow(() -> question.answer(answers));

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Weight was invalid"));
    }

    @Test
    void testDETAILSEmptyWeightUsesDefault() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = getCorrectDetails();
        answers.setProperty("weight", "");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        assertDoesNotThrow(() -> question.answer(answers));

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlNotEqual());
        assertThat(gm, didNotReceiveMessageContaining("Price was invalid"));
        Details details = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("details"));
        assertEquals(1200, details.weight);
    }

    @Test
    void testDETAILSAuxInvalidReshowsQuestion() {
        Properties answers = getCorrectDetails();
        answers.setProperty("aux", "abc");
        answers.setProperty("RESTOCKING", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(DETAILS);
        question.sendQuestion();
        question.answer(answers);

        assertEquals(2, factory.getCommunicator(gm).getBml().length);
        assertThat(gm, bmlEqual());
        assertThat(gm, receivedMessageContaining("Aux Byte was invalid."));
    }

    @Test
    void testENCHANTMENTSApplyPowerChanges() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        answers.setProperty("p0", "2.0");
        answers.setProperty("ENCHANTMENTS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ENCHANTMENTS);
        Enchantments enchantments = new Enchantments();
        Spell spell = Spells.getSpellsEnchantingItems()[0];
        enchantments.setPower(spell, 1.0f);
        ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"), enchantments);
        question.answer(answers);

        enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        Enchantment enchantment = enchantments.iterator().next();
        assertEquals(spell, enchantment.spell);
        assertEquals(2f, enchantment.power);
    }

    @Test
    void testENCHANTMENTSRemoveEnchantment() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        answers.setProperty("r1", "true");
        answers.setProperty("p0", "2.0");
        answers.setProperty("ENCHANTMENTS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ENCHANTMENTS);
        Enchantments enchantments = new Enchantments();
        Spell spell1 = Spells.getSpellsEnchantingItems()[0];
        Spell spell2 = Spells.getSpellsEnchantingItems()[1];
        enchantments.setPower(spell1, 1.0f);
        enchantments.setPower(spell2, 2.0f);
        ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"), enchantments);
        question.answer(answers);

        enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        assertEquals(1, enchantments.toList().size());
        Enchantment enchantment = enchantments.iterator().next();
        assertEquals(spell1, enchantment.spell);
        assertEquals(2f, enchantment.power);
    }

    @Test
    void testENCHANTMENTSRemoveLastEnchantment() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        answers.setProperty("r0", "true");
        answers.setProperty("ENCHANTMENTS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ENCHANTMENTS);
        Enchantments enchantments = new Enchantments();
        Spell spell1 = Spells.getSpellsEnchantingItems()[0];
        enchantments.setPower(spell1, 1.0f);
        ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"), enchantments);
        question.answer(answers);

        enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        assertEquals(0, enchantments.toList().size());
    }

    @Test
    void testENCHANTMENTSRPStartsAt0() throws NoSuchFieldException, IllegalAccessException {
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ENCHANTMENTS);
        Enchantments enchantments = new Enchantments();
        Spell spell1 = Spells.getSpellsEnchantingItems()[0];
        enchantments.setPower(spell1, 1.0f);
        ReflectionUtil.setPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"), enchantments);
        question.answer(new Properties());

        assertThat(gm, receivedBMLContaining("p0"));
        assertThat(gm, receivedBMLContaining("r0"));
    }

    @Test
    void testADD_ENCHANTMENTBack() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        answers.setProperty("back", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ADD_ENCHANTMENT);
        question.answer(answers);
        getQuestionAtStage(ENCHANTMENTS).sendQuestion();

        Enchantments enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        assertEquals(0, enchantments.toList().size());
        assertThat(gm, bmlEqual());
    }

    @Test
    void testADD_ENCHANTMENTAdd() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        Spell spell = Enchantments.allEnchantments[2];
        answers.setProperty("enchant", String.valueOf(2));
        answers.setProperty("power", "123");
        answers.setProperty("ENCHANTMENTS", "true");
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ADD_ENCHANTMENT);
        question.answer(answers);
        getQuestionAtStage(ENCHANTMENTS).sendQuestion();

        Enchantments enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        assertEquals(1, enchantments.toList().size());
        Enchantment enchantment = enchantments.iterator().next();
        assertEquals(spell, enchantment.spell);
        assertEquals(123f, enchantment.power);
        assertThat(gm, bmlNotEqual());
    }

    @Test
    void testADD_ENCHANTMENTAddSameAsQL() throws NoSuchFieldException, IllegalAccessException {
        Properties answers = new Properties();
        Spell spell = Enchantments.allEnchantments[2];
        CustomTraderItemsConfigurationQuestion question = getQuestionAtStage(ADD_ENCHANTMENT);
        answers.setProperty("enchant", String.valueOf(2));
        float ql = ((Details)ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("details"))).ql;
        answers.setProperty("power", String.valueOf(ql));
        answers.setProperty("ENCHANTMENTS", "true");

        question.answer(answers);
        getQuestionAtStage(ENCHANTMENTS).sendQuestion();

        Enchantments enchantments = ReflectionUtil.getPrivateField(question, CustomTraderItemsConfigurationQuestion.class.getDeclaredField("enchantments"));
        assertEquals(1, enchantments.toList().size());
        Enchantment enchantment = enchantments.iterator().next();
        assertEquals(spell, enchantment.spell);
        assertEquals(ql, enchantment.power);
        assertThat(gm, bmlNotEqual());
    }

    @Test
    void testEligibleMaterialsIncludesMissingWoodTypes() throws NoSuchTemplateException {
        EligibleMaterials materials = new EligibleMaterials(ItemTemplateFactory.getInstance().getTemplate(ItemList.log));
        assertTrue(materials.getOptions().contains(ItemMaterials.WOOD_ORANGE_MATERIAL_STRING));
        assertTrue(materials.getOptions().contains(ItemMaterials.WOOD_LINGONBERRY_MATERIAL_STRING));
    }
}
