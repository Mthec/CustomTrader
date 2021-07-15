package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static mod.wurmunlimited.npcs.ModelSetter.HUMAN_MODEL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CustomTraderManagementQuestionTests extends CustomTraderTest {
    private Player gm;
    private Creature trader;
    private static final String tag = "MyTag";
    private static final String differentTag = "different";

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        gm = factory.createNewPlayer();
        trader = factory.createNewCustomTrader();
    }

    @Test
    void testProperlyGetsCurrentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);

        new CustomTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining(tag));
    }

    @Test
    void testContainsAllTags() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        new CustomTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("tag1,tag2,tag3"));
    }

    @Test
    void testProperlyDePrefixesName() {
        CustomTraderMod.namePrefix = "MyPrefix";
        trader.setName("MyPrefix_Fred");
        new CustomTraderManagementQuestion(gm, trader).sendQuestion();
        assertThat(gm, receivedBMLContaining("Name: MyPrefix_"));
        assertThat(gm, receivedBMLContaining("text=\"Fred\";id=\"name\";"));
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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
        assertEquals(1, trader.getInventory().getItems().size());
        assertEquals(name, trader.getName());
    }

    @Test
    void testCustomiseSent() throws SQLException {
        CustomTraderMod.mod.modelSetter.setModelFor(trader, HUMAN_MODEL_NAME);
        long oldFace = 112358;
        CustomTraderMod.mod.faceSetter.setFaceFor(trader, oldFace);
        Properties properties = new Properties();
        properties.setProperty("customise", "true");
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        new CreatureCustomiserQuestion(gm, trader, CustomTraderMod.mod.faceSetter, CustomTraderMod.mod.modelSetter, modelOptions).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testSetTag() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", tag);
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetDifferentTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", differentTag);
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(differentTag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testSetTagFromDropdown() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tags", "1");
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
        assertThat(gm, receivedMessageContaining("tag was set"));
    }

    @Test
    void testRemoveTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(trader, tag);
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("tag", "");
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);
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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);
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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.coin).sendQuestion();

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);
        new CustomTraderItemList(gm, trader, PaymentType.coin).sendQuestion();

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

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
        new CustomTraderManagementQuestion(gm, trader).answer(properties);

        assertEquals(3, factory.getAllCreatures().size());
        assertThat(gm, receivedMessageContaining("is trading"));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }
}
