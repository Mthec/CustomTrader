package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Favor;
import mod.wurmunlimited.npcs.customtrader.stats.Health;
import mod.wurmunlimited.npcs.customtrader.stats.Karma;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static mod.wurmunlimited.npcs.ModelSetter.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class StatTraderManagementQuestionTests extends CustomTraderTest {
    private Player gm;
    private Creature trader;
    private Stat stat;
    private static final String tag = "MyTag";
    private static final String differentTag = "different";

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        gm = factory.createNewPlayer();
        stat = create(Karma.class.getSimpleName(), 1.0f);
        trader = factory.createNewStatTrader(stat);
    }

    private int indexOf(String name) {
        return Stat.getAll().indexOf(Stat.getFactoryByName(name));
    }

    @Test
    void testProperlyGetsCurrentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining(tag));
    }

    @Test
    void testContainsAllTags() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewStatTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewStatTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewStatTrader(), "tag3");

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("tag1,tag2,tag3"));
    }

    @Test
    void testProperlyGetFace() throws SQLException {
        long face = 24680;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, face);
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining(face + "\";id=\"face\""));
    }

    @Test
    void testProperlyGetFaceIfNotHuman() throws SQLException {
        assert CustomTraderMod.mod.faceSetter.getFaceFor(trader) == null;
        CustomTraderMod.mod.modelSetter.setModelFor(trader, TRADER_MODEL_NAME);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("\"\";id=\"face\""));
    }

    @Test
    void testProperlyGetsModelTrader() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, TRADER_MODEL_NAME);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"default\";text=\"Trader\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining("text=\"\";id=\"custom_model\""));
    }

    @Test
    void testProperlyGetsModelHuman() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"human\";text=\"Human\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining("text=\"\";id=\"custom_model\""));
    }

    @Test
    void testProperlyGetsModel() throws SQLException {
        String model = "custom.model";
        CustomTraderMod.mod.modelSetter.setModelFor(trader, model);

        new StatTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("id=\"custom\";text=\"Custom\";selected=\"true\""));
        assertThat(gm, receivedBMLContaining(model + "\";id=\"custom_model\""));
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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("Trader_" + name, trader.getName());
        assertThat(gm, didNotReceiveMessageContaining("will now be known as " + name));
        assertThat(gm, didNotReceiveMessageContaining("will remain " + name));
    }

    @Test
    void testNothingChangesIfNoSettingsAreAltered() throws CustomTraderDatabase.StockUpdateException {
        assert CustomTraderDatabase.getTagFor(trader).equals("");
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        CustomTraderDatabase.restock(trader);
        String name = trader.getName();

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name.substring(7));
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
        assertEquals(1, trader.getInventory().getItems().size());
        assertEquals(stat, CustomTraderDatabase.getStatFor(trader));
        assertEquals(name, trader.getName());
    }

    @Test
    void testCustomizeFaceSent() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);
        long oldFace = 112358;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, oldFace);
        Properties properties = new Properties();
        properties.setProperty("face", "");
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(oldFace, (long)CustomTraderMod.mod.faceSetter.getFaceFor(trader));
        assertNotNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, didNotReceiveMessageContaining("Invalid"));
    }

    @Test
    void testFaceChanged() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);
        long newFace = 112358;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, newFace + 1);

        Properties properties = new Properties();
        properties.setProperty("face", Long.toString(newFace));
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(newFace, (long)CustomTraderMod.mod.faceSetter.getFaceFor(trader));
        assertNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, didNotReceiveMessageContaining("Invalid"));
    }

    @Test
    void testInvalidFace() throws SQLException {
        long oldFace = 112358;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, oldFace);
        Properties properties = new Properties();
        properties.setProperty("face", "abc");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(oldFace, (long)CustomTraderMod.mod.faceSetter.getFaceFor(trader));
        assertNull(factory.getCommunicator(gm).sendCustomizeFace);
        assertThat(gm, receivedMessageContaining("Invalid"));
    }

    @Test
    void testAsksForFaceIfModelSetHuman() throws SQLException {
        String oldModel = "old.model";
        CustomTraderMod.mod.modelSetter.setModelFor(trader, oldModel);
        assert CustomTraderMod.mod.faceSetter.getFaceFor(trader) == null;
        Properties properties = new Properties();
        properties.setProperty("model", "human");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(HUMAN_MODEL_NAME, CustomTraderMod.mod.modelSetter.getModelFor(trader));
        assertNotNull(factory.getCommunicator(gm).sendCustomizeFace);
    }

    @Test
    void testModelSetTrader() throws SQLException {
        String oldModel = "old.model";
        CustomTraderMod.mod.modelSetter.setModelFor(trader, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "default");
        properties.setProperty("custom_model", "blah");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(TRADER_MODEL_NAME, CustomTraderMod.mod.modelSetter.getModelFor(trader));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testModelSetHuman() throws SQLException {
        String oldModel = "old.model";
        CustomTraderMod.mod.modelSetter.setModelFor(trader, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "human");
        properties.setProperty("custom_model", "blah");
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(HUMAN_MODEL_NAME, CustomTraderMod.mod.modelSetter.getModelFor(trader));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testModelSetCustom() throws SQLException {
        String oldModel = "old.model";
        String customModel = "custom.model";
        CustomTraderMod.mod.modelSetter.setModelFor(trader, oldModel);
        Properties properties = new Properties();
        properties.setProperty("model", "custom");
        properties.setProperty("custom_model", customModel);
        properties.setProperty("confirm", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(customModel, CustomTraderMod.mod.modelSetter.getModelFor(trader));
        assertThat(gm, receivedMessageContaining(MODEL_CHANGE_SUCCESS));
        assertThat(gm, didNotReceiveMessageContaining(MODEL_CHANGE_FAILURE));
    }

    @Test
    void testSetTag() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetDifferentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(differentTag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetTagFromDropdown() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

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
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testRemoveTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("unique inventory"));
    }

    @Test
    void testRemoveTagRemovesItems() throws CustomTraderDatabase.StockUpdateException, CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.addStockItemTo(tag, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 0);
        CustomTraderDatabase.updateTag(trader, tag);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("unique inventory"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testSetTagRemovesItems() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 0);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testSetDropdownTagRemovesItems() throws CustomTraderDatabase.StockUpdateException, CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 0);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == 1;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertThat(gm, receivedMessageContaining("tag was set"));
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testEmptyRemovesAllItemsFromInventory() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 10, 1, 0);
        CustomTraderDatabase.fullyStock(trader);
        trader.getInventory().insertItem(factory.createNewItem());

        assert trader.getInventory().getItems().size() == 11;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("empty", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(0, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("got rid"));
    }

    @Test
    void testFullyRestockFillsInventoryWithItems() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 10, 1, 0);
        assert trader.getInventory().getItems().size() == 0;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("full", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(10, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("fully"));
    }

    @Test
    void testEmptyAndFullyRestock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 10, 1, 0);
        CustomTraderDatabase.fullyStock(trader);
        trader.getInventory().insertItem(factory.createNewItem());

        assert trader.getInventory().getItems().size() == 11;
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("empty", "true");
        properties.setProperty("full", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(10, trader.getInventory().getItems().size());
        assertThat(gm, receivedMessageContaining("got rid"));
        assertThat(gm, receivedMessageContaining("fully"));
    }

    @Test
    void testEditTagsButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderEditTags(gm).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testEditTagsButtonSelectedDoesNotChangeTagOrDeleteStock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("edit", "true");
        properties.setProperty("delete", "true");
        properties.setProperty("tag", tag);
        new StatTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderEditTags(gm).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testListButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("list", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.other).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testListButtonSelectedDoesNotChangeTagOrDeleteStock() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;

        Properties properties = new Properties();
        properties.setProperty("list", "true");
        properties.setProperty("delete", "true");
        properties.setProperty("tag", tag);
        new StatTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.other).sendQuestion();

        assertThat(gm, bmlEqual());
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testDismissButtonSelected() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;
        assert factory.getAllCreatures().size() == 2;

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(1, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("dismiss"));
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testCannotDismissIfTraderIsTrading() throws CustomTraderDatabase.StockUpdateException {
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[0], (byte)0, 1, 1, 1);
        assert CustomTraderDatabase.getStockFor(trader).length == 1;
        assert factory.getAllCreatures().size() == 2;

        trader.setTrade(new Trade(factory.createNewPlayer(), trader));

        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(3, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("is trading"));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testSetStat() {
        assert indexOf(stat.name) != 1;
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("stat", "1");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        Stat newStat = CustomTraderDatabase.getStatFor(trader);
        assertNotNull(newStat);
        assertNotEquals(stat.name, newStat.name);
        assertEquals(stat.ratio, newStat.ratio);
        assertThat(gm, receivedMessageContaining("will now collect"));
    }

    @Test
    void testSetRatio() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("ratio", "0.25");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        Stat newStat = CustomTraderDatabase.getStatFor(trader);
        assertNotNull(newStat);
        assertEquals(stat.name, newStat.name);
        assertNotEquals(stat.ratio, newStat.ratio);
        assertThat(gm, receivedMessageContaining("will now collect"));
    }

    @Test
    void testRatioMustBePositive() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("ratio", "0.0");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        Stat newStat = CustomTraderDatabase.getStatFor(trader);
        assertNotNull(newStat);
        assertEquals(stat.name, newStat.name);
        assertEquals(stat.ratio, newStat.ratio);
        assertThat(gm, didNotReceiveMessageContaining("will now collect"));
    }

    @Test
    void testSetStatAndRatio() {
        assert indexOf(stat.name) != 1;
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("stat", "1");
        properties.setProperty("ratio", "0.25");
        new StatTraderManagementQuestion(gm, trader).answer(properties);

        Stat newStat = CustomTraderDatabase.getStatFor(trader);
        assertNotNull(newStat);
        assertNotEquals(stat.name, newStat.name);
        assertNotEquals(stat.ratio, newStat.ratio);
        assertThat(gm, receivedMessageContaining("will now collect"));
    }

    @Test
    void testCurrentStatSetProperly() {
        int i = indexOf(Health.class.getSimpleName());
        if (i != 0) {
            CustomTraderDatabase.setStatFor(trader, create(Health.class.getSimpleName(), 1.25f));
        } else {
            i = indexOf(Favor.class.getSimpleName());
            CustomTraderDatabase.setStatFor(trader, create(Favor.class.getSimpleName(), 1.25f));
        }

        new StatTraderManagementQuestion(gm, trader).sendQuestion();

        assertThat(gm, receivedBMLContaining("default=\"" + i + "\""));
    }
}
