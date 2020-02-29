package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.spells.Spells;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.npcs.customtrader.CustomTraderDatabaseAssertions.*;
import static mod.wurmunlimited.npcs.customtrader.DatabaseActions.select;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CustomTraderDatabaseTests extends CustomTraderTest {

    @Test
    void testAddNew() {
        Creature trader = factory.createNewCustomTrader();

        assertThat(trader, isInDb());
    }

    // Tags

    @Test
    void testTagProperlySet() {
        String tag = "testTagProperlySet";
        Creature trader = factory.createNewCustomTrader(tag);

        assertThat(trader, hasTag(tag));
    }

    @Test
    void testEmptyTagProperlySet() {
        String tag = "";
        Creature trader = factory.createNewCustomTrader(tag);

        assertThat(trader, hasTag(tag));
    }

    @Test
    void testTagChangedProperly() throws CustomTraderDatabase.FailedToUpdateTagException {
        String tag = "testTagChangedProperly";
        Creature trader = factory.createNewCustomTrader(tag);

        assertThat(trader, hasTag(tag));

        CustomTraderDatabase.updateTag(trader, tag + "blah");

        assertThat(trader, doesNotHaveTag(tag));
    }

    @Test
    void testLastUpdateBeforeCurrentTime() {
        Creature trader = factory.createNewCustomTrader();

        assertTrue(((Integer)select("last_restock", "id", String.valueOf(trader.getWurmId()), "last_restock")) < System.currentTimeMillis());
    }

    @Test
    void testDeleteTag() {
        String tag = "test";
        Creature trader = factory.createNewCustomTrader(tag);

        assertDoesNotThrow(() -> CustomTraderDatabase.deleteTag(tag));
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testDeleteTagAlsoRemovesItemsFromAssociatedTraders() throws CustomTraderDatabase.StockUpdateException {
        int num = 5;
        byte b = 0;
        String tag = "test";
        Creature trader = factory.createNewCustomTrader(tag);
        Creature secondTrader = factory.createNewCustomTrader(tag);

        CustomTraderDatabase.addStockItemTo(tag, num + 1, num + 1, num + 1, b, b, new Enchantment[0], num + 1, num + 1, num + 1);
        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.restock(trader);
        assert trader.getInventory().getItems().size() == num * 2 + 1;

        assertDoesNotThrow(() -> CustomTraderDatabase.deleteTag(tag));
        assertEquals("", CustomTraderDatabase.getTagFor(trader));
        assertEquals(0, CustomTraderDatabase.getStockFor(trader).length);
        assertEquals(0, trader.getInventory().getItems().size());
        assertEquals(0, secondTrader.getInventory().getItems().size());
    }

    @Test
    void testDeleteNoneExistentTag() {
        String tag = "test";
        factory.createNewCustomTrader("otherTag");

        assertDoesNotThrow(() -> CustomTraderDatabase.deleteTag(tag));
    }

    @Test
    void testRenameTag() throws CustomTraderDatabase.StockUpdateException {
        String tag = "test";
        String newTag = "newTag";
        Creature trader = factory.createNewCustomTrader(tag);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)1, (byte)1, new Enchantment[0], 1, 1, 1);

        assertDoesNotThrow(() -> CustomTraderDatabase.renameTag(tag, newTag));
        assertEquals(newTag, CustomTraderDatabase.getTagFor(trader));
        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testRenameNoneExistentTag() {
        String tag = "test";
        factory.createNewCustomTrader("otherTag");

        assertDoesNotThrow(() -> CustomTraderDatabase.renameTag(tag, "what?"));
    }

    @Test
    void testGetTagFor() {
        String tag = "testGetTagFor";
        Creature trader = factory.createNewCustomTrader(tag);

        assertEquals(tag, CustomTraderDatabase.getTagFor(trader));
    }

    @Test
    void testEmptyStringReturnedWhenTagNotFound() {
        Creature notCustomTrader = factory.createNewTrader();

        assertEquals("", CustomTraderDatabase.getTagFor(notCustomTrader));
    }

    @Test
    void testGetAllTags() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
    }

    @Test
    void testGetAllTagsInAlphabeticalOrder() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "b1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "a1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "c1");

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals("a1", allTags.get(0));
        assertEquals("b1", allTags.get(1));
        assertEquals("c1", allTags.get(2));
    }

    @Test
    void testGetAllTagsDoesNotThrowErrorWhenEmpty() {
        assert CustomTraderDatabase.getAllTags().size() == 0;
        assertDoesNotThrow(CustomTraderDatabase::getAllTags);
    }

    @Test
    void testEmptyTagNotIncludedInResults() {
        factory.createNewCustomTrader();

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(0, allTags.size());
    }

    // Stock

    @Test
    void testAddGetStockItemToUniqueTrader() throws CustomTraderDatabase.StockUpdateException {
        Creature uniqueTrader = factory.createNewCustomTrader();
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(uniqueTrader, 0, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.addStockItemTo(uniqueTrader, 1, num, num, b, b, new Enchantment[0], num, num, num);

        StockInfo[] allStock = CustomTraderDatabase.getStockFor(uniqueTrader);
        assertEquals(2, allStock.length);

        int count = 0;
        for (StockInfo stock : allStock) {
            assertEquals(count++, stock.item.templateId);
            assertEquals(num, stock.item.ql, 0.001f);
            assertEquals(num, stock.item.price);
            assertEquals(b, stock.item.material);
            assertEquals(b, stock.item.rarity);
            assertEquals(num, stock.maxNum);
            assertEquals(num, stock.restockRate);
            assertEquals(num, stock.restockInterval);
        }
    }

    @Test
    void testAddGetStockItemToTag() throws CustomTraderDatabase.StockUpdateException {
        String tag = "test";
        Creature tagTrader = factory.createNewCustomTrader(tag);
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(tag, 0, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.addStockItemTo(tag, 1, num, num, b, b, new Enchantment[0], num, num, num);

        StockInfo[] allStock = CustomTraderDatabase.getStockFor(tagTrader);
        assertEquals(2, allStock.length);

        int count = 0;
        for (StockInfo stock : allStock) {
            assertEquals(count++, stock.item.templateId);
            assertEquals(num, stock.item.ql, 0.001f);
            assertEquals(num, stock.item.price);
            assertEquals(b, stock.item.material);
            assertEquals(b, stock.item.rarity);
            assertEquals(num, stock.maxNum);
            assertEquals(num, stock.restockRate);
            assertEquals(num, stock.restockInterval);
        }
    }

    @Test
    void testRemoveStockItemFromUniqueTrader() throws CustomTraderDatabase.StockUpdateException {
        Creature uniqueTrader = factory.createNewCustomTrader();
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num + 1, num + 1, num + 1, b, b, new Enchantment[0], num + 1, num + 1, 0);
        StockInfo toRemove = CustomTraderDatabase.getStockFor(uniqueTrader)[0];
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);
        CustomTraderDatabase.restock(uniqueTrader);
        assert uniqueTrader.getInventory().getItems().size() == num * 2 + 1;

        CustomTraderDatabase.removeStockItemFrom(uniqueTrader, toRemove);
        StockInfo[] allStock = CustomTraderDatabase.getStockFor(uniqueTrader);
        assertEquals(1, allStock.length);

        StockInfo stock = allStock[0];
        assertEquals(num, stock.item.templateId);
        assertEquals(num, stock.item.ql, 0.001f);
        assertEquals(num, stock.item.price);
        assertEquals(b, stock.item.material);
        assertEquals(b, stock.item.rarity);
        assertEquals(num, stock.maxNum);
        assertEquals(num, stock.restockRate);
        assertEquals(0, stock.restockInterval);
        assertEquals(num, uniqueTrader.getInventory().getItems().size());
    }

    @Test
    void testRemoveStockItemFromTag() throws CustomTraderDatabase.StockUpdateException {
        String tag = "test";
        Creature tagTrader = factory.createNewCustomTrader(tag);
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(tag, num + 1, num + 1, num + 1, b, b, new Enchantment[0], num + 1, num + 1, num + 1);
        StockInfo toRemove = CustomTraderDatabase.getStockFor(tagTrader)[0];
        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.restock(tagTrader);
        assert tagTrader.getInventory().getItems().size() == num * 2 + 1;

        CustomTraderDatabase.removeStockItemFrom(tagTrader, toRemove);
        StockInfo[] allStock = CustomTraderDatabase.getStockFor(tagTrader);
        assertEquals(1, allStock.length);

        StockInfo stock = allStock[0];
        assertEquals(num, stock.item.templateId);
        assertEquals(num, stock.item.ql, 0.001f);
        assertEquals(num, stock.item.price);
        assertEquals(b, stock.item.material);
        assertEquals(b, stock.item.rarity);
        assertEquals(num, stock.maxNum);
        assertEquals(num, stock.restockRate);
        assertEquals(num, stock.restockInterval);
        assertEquals(num, tagTrader.getInventory().getItems().size());
    }

    @Test
    void testDeleteAllStockFor() throws CustomTraderDatabase.StockUpdateException {
        Creature uniqueTrader = factory.createNewCustomTrader();
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num + 1, num + 1, num + 1, b, b, new Enchantment[0], num + 1, num + 1, num + 1);
        CustomTraderDatabase.restock(uniqueTrader);

        assert CustomTraderDatabase.getStockFor(uniqueTrader).length == 2;
        assert uniqueTrader.getInventory().getItems().size() > 0;

        CustomTraderDatabase.deleteAllStockFor(uniqueTrader);
        StockInfo[] allStock = CustomTraderDatabase.getStockFor(uniqueTrader);
        assertEquals(0, allStock.length);
        assertEquals(0, uniqueTrader.getInventory().getItems().size());
    }

    @Test
    void testUniqueAppliesToEnchantments() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        int num = 5;
        byte b = 0;
        Enchantment[] enchantments = new Enchantment[] {
                new Enchantment(Spells.SPELL_CIRCLE_OF_CUNNING, 100f),
                new Enchantment(Spells.SPELL_WIND_OF_AGES, 200f)
        };
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, enchantments, num, num, num);
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, enchantments, num + 1, num + 1, num + 1);
        CustomTraderDatabase.restock(trader);

        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }
}
