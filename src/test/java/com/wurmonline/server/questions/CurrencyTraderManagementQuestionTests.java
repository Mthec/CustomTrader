package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.items.*;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static mod.wurmunlimited.npcs.ModelSetter.HUMAN_MODEL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CurrencyTraderManagementQuestionTests extends CustomTraderTest {
    private Player gm;
    private Creature trader;
    private static final String tag = "MyTag";
    private static final String differentTag = "different";

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        gm = factory.createNewPlayer();
        trader = factory.createNewCurrencyTrader(ItemList.acorn);
    }

    @Test
    void testProperlyGetsCurrentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);

        new CurrencyTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining(tag));
    }

    @Test
    void testContainsAllTags() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCurrencyTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCurrencyTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCurrencyTrader(), "tag3");

        new CurrencyTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("tag1,tag2,tag3"));
    }

    @Test
    void testCurrentCurrencySetProperly() throws NoSuchTemplateException {
        CustomTraderDatabase.setCurrencyFor(trader, new Currency(ItemTemplateFactory.getInstance().getTemplate(ItemList.sprout)));
        new CurrencyTraderManagementQuestion(gm, trader).sendQuestion();

        assertThat(gm, receivedBMLContaining("default=\"" + new EligibleTemplates("").getIndexOf(ItemList.sprout) + "\""));
    }

    // answer

    @Test
    void testSetName() {
        assert CustomTraderMod.namePrefix.equals("Trader");
        String name = StringUtilities.raiseFirstLetter("MyName");
        String newName = "Trader_" + name;
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(newName, trader.getName());
        assertEquals(newName, ((FakeCreatureStatus)trader.getStatus()).savedName);
        assertThat(gm, receivedMessageContaining("will now be known as " + newName));
    }

    @Test
    void testSetNameDifferentPrefix() {
        CustomTraderMod.namePrefix = "MyPrefix";
        assert !trader.getName().startsWith(CustomTraderMod.namePrefix);
        String name = StringUtilities.raiseFirstLetter("MyName");
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("MyPrefix_" + name, trader.getName());
        assertThat(gm, receivedMessageContaining("will now be known as MyPrefix_" + name));
    }

    @Test
    void testSetNameIllegalCharacters() {
        assert CustomTraderMod.namePrefix.equals("Trader");
        String name = trader.getName();
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "%Name");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertNotEquals(name, trader.getName());
        assertThat(gm, receivedMessageContaining(name + " didn't like that name"));
        assertThat(gm, receivedMessageContaining(name + " will now be known as "));
    }

    @Test
    void testSetNameRandomWhenBlank() {
        assert CustomTraderMod.namePrefix.equals("Trader");
        String name = trader.getName();
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertNotEquals(name, trader.getName());
        assertTrue(trader.getName().startsWith("Trader_"));
        assertThat(gm, receivedMessageContaining(name + " chose a new name"));
        assertThat(gm, receivedMessageContaining(name + " will now be known as "));
    }

    @Test
    void testSetNameNoMessageOnSameName() {
        assert CustomTraderMod.namePrefix.equals("Trader");
        String name = "Name";
        trader.setName("Trader_" + name);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("Trader_" + name, trader.getName());
        assertThat(gm, didNotReceiveMessageContaining("will now be known as " + name));
    }

    @Test
    void testNothingChangesIfNoSettingsAreAltered() throws CustomTraderDatabase.StockUpdateException {
        assert CustomTraderDatabase.getTagFor(trader).equals("");
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        CustomTraderDatabase.restock(trader);
        String name = trader.getName();

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name.substring(7));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
        assertEquals(1, trader.getInventory().getItems().size());
        assertEquals(ItemList.acorn, Objects.requireNonNull(CustomTraderDatabase.getCurrencyFor(trader)).templateId);
        assertEquals(name, trader.getName());
    }

    @Test
    void testCustomiseSent() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);
        long oldFace = 112358;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, oldFace);
        Properties properties = new Properties();
        properties.setProperty("customise", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        new CreatureCustomiserQuestion(gm, trader, CustomTraderMod.mod.faceSetter, CustomTraderMod.mod.modelSetter, modelOptions).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testSetTag() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetDifferentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(differentTag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetTagFromDropdown() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testDropdownTagOverridesManualTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        properties.setProperty("tags", "1");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testRemoveTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("unique inventory"));
    }

    @Test
    void testRemoveTagRemovesItems() throws CustomTraderDatabase.StockUpdateException, CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.addStockItemTo(tag, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 0);
        CustomTraderDatabase.updateTag(trader, tag);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("unique inventory"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testSetTagRemovesItems() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 0);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testSetDropdownTagRemovesItems() throws CustomTraderDatabase.StockUpdateException, CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 0);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testEmptyRemovesAllItemsFromInventory() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 10, 1, 0);
        CustomTraderDatabase.fullyStock(trader);
        trader.getInventory().insertItem(factory.createNewItem());

        assert trader.getInventory().getItems().size() == 11;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("empty", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(0, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("got rid"));
    }

    @Test
    void testFullyRestockFillsInventoryWithItems() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 10, 1, 0);
        assert trader.getInventory().getItems().size() == 0;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("full", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(10, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("fully"));
    }

    @Test
    void testEmptyAndFullyRestock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 10, 1, 0);
        CustomTraderDatabase.fullyStock(trader);
        trader.getInventory().insertItem(factory.createNewItem());

        assert trader.getInventory().getItems().size() == 11;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("empty", "true");
        properties.setProperty("full", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(10, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("got rid"));
        assertThat(gm, receivedMessageContaining("fully"));
    }

    @Test
    void testEditTagsButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderEditTags(gm).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testEditTagsButtonSelectedDoesNotChangeTagOrDeleteStock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("delete", "true");
        properties.setProperty("tag", tag);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderEditTags(gm).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testListButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("list", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.currency).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testListButtonSelectedDoesNotChangeTagOrDeleteStock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("list", "true");
        properties.setProperty("tag", tag);
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.currency).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testDismissButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;
        assert factory.getAllCreatures().size() == 2;

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(1, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("dismiss"));
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testCannotDismissIfTraderIsTrading() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, "", 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;
        assert factory.getAllCreatures().size() == 2;

        trader.setTrade(new Trade(factory.createNewPlayer(), trader));

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(3, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("is trading"));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testSetCurrency() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        int templateIndex = 101;
        EligibleTemplates.init();
        EligibleTemplates template = new EligibleTemplates("");
        int templateId = template.getTemplate(templateIndex).getTemplateId();
        properties.setProperty("template", String.valueOf(templateIndex));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(templateId, Objects.requireNonNull(CustomTraderDatabase.getCurrencyFor(trader)).templateId);
        assertThat(gm, receivedMessageContaining("currency was set"));
    }

    @Test
    void testSetCurrencyInvalidMaterial() throws NoSuchTemplateException {
        CustomTraderDatabase.setCurrencyFor(trader, new Currency(ItemList.carrot, -1f, -1f, Materials.MATERIAL_VEGETARIAN, (byte)-1, true));
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        EligibleTemplates.init();
        EligibleTemplates template = new EligibleTemplates("");
        int templateId = template.getTemplate(template.getIndexOf(ItemList.carrot)).getTemplateId();
        properties.setProperty("template", String.valueOf(template.getIndexOf(ItemList.pickAxe)));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        Currency currency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(currency);
        assertEquals(ItemList.pickAxe, currency.templateId);
        assertNotEquals(Materials.MATERIAL_VEGETARIAN, currency.material);
        assertThat(gm, receivedMessageContaining("currency material was changed"));
    }

    @Test
    void testSetCurrencyValidMaterial() throws NoSuchTemplateException {
        CustomTraderDatabase.setCurrencyFor(trader, new Currency(ItemList.carrot, -1f, -1f, Materials.MATERIAL_VEGETARIAN, (byte)-1, true));
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        EligibleTemplates.init();
        EligibleTemplates template = new EligibleTemplates("");
        int templateId = template.getTemplate(template.getIndexOf(ItemList.carrot)).getTemplateId();
        properties.setProperty("template", String.valueOf(template.getIndexOf(ItemList.cabbage)));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);

        Currency currency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(currency);
        assertEquals(ItemList.cabbage, currency.templateId);
        assertEquals(Materials.MATERIAL_VEGETARIAN, currency.material);
        assertThat(gm, didNotReceiveMessageContaining("currency material was changed"));
        assertThat(gm, receivedMessageContaining("currency was set"));
    }

    @Test
    void testAdvancedButtonSameTemplate() throws NoSuchTemplateException {
        Currency currency = new Currency(ItemList.carrot, -1f, -1f, Materials.MATERIAL_VEGETARIAN, (byte)-1, true);
        CustomTraderDatabase.setCurrencyFor(trader, currency);
        String otherName = "blah";
        String otherTag = "blah";
        Properties properties = new Properties();
        properties.setProperty("name", otherName);
        properties.setProperty("tag", otherTag);
        properties.setProperty("empty", "true");
        properties.setProperty("full", "true");
        properties.setProperty("advanced", "true");
        EligibleTemplates.init();
        EligibleTemplates template = new EligibleTemplates("");
        properties.setProperty("template", String.valueOf(template.getIndexOf(ItemList.carrot)));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).sendQuestion();

        assertEquals(currency.templateId, Objects.requireNonNull(CustomTraderDatabase.getCurrencyFor(trader)).templateId);
        assertThat(gm, bmlEqual());
        assertNotEquals(otherName, trader.getName());
        assertNotEquals(otherTag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, didNotReceiveMessageContaining("got rid of"));
        assertThat(gm, didNotReceiveMessageContaining("fully stocked"));
    }

    @Test
    void testAdvancedButtonDifferentTemplate() throws NoSuchTemplateException {
        Currency currency = new Currency(ItemList.carrot, -1f, -1f, Materials.MATERIAL_VEGETARIAN, (byte)-1, true);
        CustomTraderDatabase.setCurrencyFor(trader, currency);
        String otherName = "blah";
        String otherTag = "blah";
        Properties properties = new Properties();
        properties.setProperty("name", otherName);
        properties.setProperty("tag", otherTag);
        properties.setProperty("empty", "true");
        properties.setProperty("full", "true");
        properties.setProperty("advanced", "true");
        EligibleTemplates.init();
        EligibleTemplates template = new EligibleTemplates("");
        properties.setProperty("template", String.valueOf(template.getIndexOf(ItemList.cabbage)));
        new CurrencyTraderManagementQuestion(gm, trader).answer(properties);
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).sendQuestion();

        assertEquals(currency.templateId, Objects.requireNonNull(CustomTraderDatabase.getCurrencyFor(trader)).templateId);
        assertThat(gm, bmlNotEqual());
        assertNotEquals(otherName, trader.getName());
        assertNotEquals(otherTag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, didNotReceiveMessageContaining("got rid of"));
        assertThat(gm, didNotReceiveMessageContaining("fully stocked"));
    }

    @Test
    void testFilterSendsNewQuestion() throws NoSuchQuestionException {
        Question question = new CurrencyTraderManagementQuestion(gm, trader);
        question.sendQuestion();

        Properties properties = new Properties();
        properties.setProperty("do_filter", "true");
        properties.setProperty("filter", "pickles");
        question.answer(properties);

        assertNotEquals(Questions.getQuestion(question.id + 1), question);
    }
}
