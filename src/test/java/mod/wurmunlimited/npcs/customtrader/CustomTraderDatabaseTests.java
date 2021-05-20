package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.spells.Spells;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.List;

import static mod.wurmunlimited.npcs.customtrader.CustomTraderDatabaseAssertions.*;
import static mod.wurmunlimited.npcs.customtrader.DatabaseActions.select;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SqlResolve")
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

        CustomTraderDatabase.addStockItemTo(tag, num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], (byte)0, num + 1, num + 1, num + 1);
        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
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
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)1, (byte)1, 1, new Enchantment[0], (byte)0, 1, 1, 1);

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

    // Currency

    @Test
    void testCurrencyProperlySet() {
        Creature trader = factory.createNewCurrencyTrader(ItemList.sprout, 10);

        assertEquals(ItemList.sprout, CustomTraderDatabase.getCurrencyFor(trader));
    }

    @Test
    void testGetCurrencyFor() {
        Creature trader = factory.createNewCurrencyTrader(ItemList.acorn, 1);

        assertEquals(ItemList.acorn, CustomTraderDatabase.getCurrencyFor(trader));
    }

    // Stock

    @Test
    void testAddGetStockItemToUniqueTrader() throws CustomTraderDatabase.StockUpdateException {
        Creature uniqueTrader = factory.createNewCustomTrader();
        int num = 5;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(uniqueTrader, 0, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
        CustomTraderDatabase.addStockItemTo(uniqueTrader, 1, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);

        StockInfo[] allStock = CustomTraderDatabase.getStockFor(uniqueTrader);
        assertEquals(2, allStock.length);

        int count = 0;
        for (StockInfo stock : allStock) {
            assertEquals(count++, stock.item.templateId);
            assertEquals(num, stock.item.ql, 0.001f);
            assertEquals(num, stock.item.price);
            assertEquals(b, stock.item.material);
            assertEquals(b, stock.item.rarity);
            assertEquals(num, stock.item.weight);
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
        CustomTraderDatabase.addStockItemTo(tag, 0, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
        CustomTraderDatabase.addStockItemTo(tag, 1, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);

        StockInfo[] allStock = CustomTraderDatabase.getStockFor(tagTrader);
        assertEquals(2, allStock.length);

        int count = 0;
        for (StockInfo stock : allStock) {
            assertEquals(count++, stock.item.templateId);
            assertEquals(num, stock.item.ql, 0.001f);
            assertEquals(num, stock.item.price);
            assertEquals(b, stock.item.material);
            assertEquals(b, stock.item.rarity);
            assertEquals(num, stock.item.weight);
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
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], (byte)0, num + 1, num + 1, 0);
        StockInfo toRemove = CustomTraderDatabase.getStockFor(uniqueTrader)[0];
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);
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
        assertEquals(num, stock.item.weight);
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
        CustomTraderDatabase.addStockItemTo(tag, num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], (byte)0, num + 1, num + 1, num + 1);
        StockInfo toRemove = CustomTraderDatabase.getStockFor(tagTrader)[0];
        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
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
        assertEquals(num, stock.item.weight);
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
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
        CustomTraderDatabase.addStockItemTo(uniqueTrader, num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], (byte)0, num + 1, num + 1, num + 1);
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
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, num, enchantments, (byte)0, num, num, num);
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, num, enchantments, (byte)0, num + 1, num + 1, num + 1);
        CustomTraderDatabase.restock(trader);

        assertEquals(1, CustomTraderDatabase.getStockFor(trader).length);
    }

    @Test
    void testAuxDataZero() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        int num = 1;
        byte b = 0;
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);
        CustomTraderDatabase.restock(trader);

        assertEquals(0, trader.getInventory().getFirstContainedItem().getAuxData());
    }

    @Test
    void testAuxDataSetProperly() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        int num = 1;
        byte b = 0;
        byte aux = 3;
        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, num, new Enchantment[0], aux, num, num, num);
        CustomTraderDatabase.restock(trader);

        assertEquals(aux, trader.getInventory().getFirstContainedItem().getAuxData());
    }

    private boolean hasAuxColumn(ResultSet rs) throws SQLException {
        boolean hasAux = false;
        while (rs.next()) {
            if (rs.getString(2).equals("aux")) {
                hasAux = true;
                break;
            }
        }
        return hasAux;
    }

    @Test
    void testOldDBUpdatesProperly() {
        execute( db -> {
            db.prepareStatement("DROP TABLE trader_stock;").execute();
            db.prepareStatement("CREATE TABLE trader_stock (" +
                                       "trader_id INTEGER," +
                                       "template_id INTEGER," +
                                       "ql REAL," +
                                       "price INTEGER," +
                                       "material INTEGER," +
                                       "rarity INTEGER," +
                                       "weight INTEGER," +
                                       "enchantments TEXT," +
                                       "max_num INTEGER," +
                                       "restock_rate INTEGER," +
                                       "restock_interval INTEGER," +
                                       "UNIQUE(trader_id, template_id, ql, material, rarity, weight, enchantments) ON CONFLICT REPLACE" +
                                       ");").execute();
            db.prepareStatement("DROP TABLE tag_stock;").execute();
            db.prepareStatement("CREATE TABLE tag_stock (" +
                                        "trader_id INTEGER," +
                                        "template_id INTEGER," +
                                        "ql REAL," +
                                        "price INTEGER," +
                                        "material INTEGER," +
                                        "rarity INTEGER," +
                                        "weight INTEGER," +
                                        "enchantments TEXT," +
                                        "max_num INTEGER," +
                                        "restock_rate INTEGER," +
                                        "restock_interval INTEGER," +
                                        "UNIQUE(trader_id, template_id, ql, material, rarity, weight, enchantments) ON CONFLICT REPLACE" +
                                        ");").execute();
            db.prepareStatement("PRAGMA user_version = 0;").execute();

            assert !hasAuxColumn(db.prepareStatement("PRAGMA table_info('trader_stock')").executeQuery());
            assert !hasAuxColumn(db.prepareStatement("PRAGMA table_info('tag_stock')").executeQuery());

            try {
                ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("init", Connection.class), db);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            ResultSet rs = db.prepareStatement("PRAGMA table_info('trader_stock');").executeQuery();
            assertTrue(hasAuxColumn(rs));
            rs = db.prepareStatement("PRAGMA table_info('tag_stock');").executeQuery();
            assertTrue(hasAuxColumn(rs));
        });
    }

    @Test
    void testDumpTagsToDb() throws SQLException, CustomTraderDatabase.StockUpdateException {
        int num = 5;
        byte b = 2;
        Creature trader = factory.createNewCustomTrader("tag");

        CustomTraderDatabase.tagDumpDbString = "sqlite/tags.db";
        CustomTraderDatabase.addStockItemTo("tag", num, num, num, b, b, num, new Enchantment[0], b, num, num, num);
        CustomTraderDatabase.addStockItemTo("tag", num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], b, num + 1, num + 1, num + 1);
        CustomTraderDatabase.dumpTags();

        StockInfo[] stock = CustomTraderDatabase.getStockFor(trader);

        execute(db -> {
            db.prepareStatement("ATTACH DATABASE '" + CustomTraderDatabase.tagDumpDbString + "' AS dump;").execute();

            ResultSet rs = db.prepareStatement("SELECT * FROM dump.tag_stock;").executeQuery();

            while (rs.next()) {
                assertEquals("tag", rs.getString(1));
                StockInfo dumped = new StockInfo(new StockItem(
                        rs.getInt(2),
                        rs.getFloat(3),
                        rs.getInt(4),
                        (byte)rs.getInt(5),
                        (byte)rs.getInt(6),
                        rs.getInt(7),
                        Enchantment.parseEnchantments(rs.getString(8)),
                        rs.getByte(12)),
                        rs.getInt(9),
                        rs.getInt(10),
                        rs.getInt(11));

                StockInfo original = stock[0];
                if (original.item.templateId != dumped.item.templateId)
                    original = stock[1];

                assertEquals(original.item.templateId, dumped.item.templateId);
                assertEquals(original.item.ql, dumped.item.ql, 0.00001f);
                assertEquals(original.item.price, dumped.item.price);
                assertEquals(original.item.material, dumped.item.material);
                assertEquals(original.item.rarity, dumped.item.rarity);
                assertEquals(original.item.weight, dumped.item.weight);
                assertArrayEquals(original.item.enchantments, dumped.item.enchantments);
                assertEquals(original.item.aux, dumped.item.aux);
                assertEquals(original.maxNum, dumped.maxNum);
                assertEquals(original.restockRate, dumped.restockRate);
                assertEquals(original.restockInterval, dumped.restockInterval);
            }

            db.prepareStatement("DETACH dump;").execute();
        });
    }

    private void createTagsDump(int num, byte b) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + CustomTraderDatabase.tagDumpDbString)) {
            db.prepareStatement("CREATE TABLE IF NOT EXISTS tag_stock (" +
                                        "tag TEXT," +
                                        "template_id INTEGER," +
                                        "ql REAL," +
                                        "price INTEGER," +
                                        "material INTEGER," +
                                        "rarity INTEGER," +
                                        "weight INTEGER," +
                                        "enchantments TEXT," +
                                        "max_num INTEGER," +
                                        "restock_rate INTEGER," +
                                        "restock_interval INTEGER," +
                                        "aux INTEGER," +
                                        "UNIQUE(tag, template_id, ql, material, rarity, weight, enchantments) ON CONFLICT REPLACE" +
                                        ");").execute();
            PreparedStatement ps = db.prepareStatement("INSERT INTO tag_stock VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, "tag");
            ps.setInt(2, num);
            ps.setFloat(3, num);
            ps.setInt(4, num);
            ps.setByte(5, b);
            ps.setByte(6, b);
            ps.setInt(7, num);
            ps.setString(8, "");
            ps.setInt(9, num);
            ps.setInt(10, num);
            ps.setInt(11, num);
            ps.setByte(12, b);
            ps.execute();
        }
    }

    @Test
    void testLoadTagsLoadsSuccessfully() throws SQLException {
        int num = 6;
        byte b = 1;
        Creature trader = factory.createNewCustomTrader("tag");
        CustomTraderDatabase.tagDumpDbString = "sqlite/tags.db";
        createTagsDump(num, b);

        CustomTraderDatabase.loadTags();

        StockInfo[] stock = CustomTraderDatabase.getStockFor(trader);

        assertEquals(1, stock.length);
        StockInfo dumped = stock[0];
        assertEquals(num, dumped.item.templateId);
        assertEquals(num, dumped.item.ql, 0.00001f);
        assertEquals(num, dumped.item.price);
        assertEquals(b, dumped.item.material);
        assertEquals(b, dumped.item.rarity);
        assertEquals(num, dumped.item.weight);
        assertEquals(b, dumped.item.aux);
        assertEquals(num, dumped.maxNum);
        assertEquals(num, dumped.restockRate);
        assertEquals(num, dumped.restockInterval);
    }

    @Test
    void testLoadTagsDoesNotDelete() throws SQLException, CustomTraderDatabase.StockUpdateException {
        int num = 6;
        byte b = 1;
        Creature trader = factory.createNewCustomTrader("tag");
        CustomTraderDatabase.tagDumpDbString = "sqlite/tags.db";
        createTagsDump(num, b);

        CustomTraderDatabase.addStockItemTo("tag", num + 1, num + 1, num + 1, b, b, num + 1, new Enchantment[0], b, num + 1, num + 1, num + 1);
        CustomTraderDatabase.loadTags();

        StockInfo[] stock = CustomTraderDatabase.getStockFor(trader);
        assertEquals(2, stock.length);

        if (stock[0].item.templateId == num) {
            assertEquals(num, stock[0].item.templateId);
            assertEquals(num + 1, stock[1].item.templateId);
        } else {
            assertEquals(num, stock[1].item.templateId);
            assertEquals(num + 1, stock[0].item.templateId);
        }
    }

    @Test
    void testTagsDumpDoesNotExist() {
        CustomTraderDatabase.tagDumpDbString = "sqlite/tags.db";
        assert !new File(CustomTraderDatabase.tagDumpDbString).exists();

        assertDoesNotThrow(CustomTraderDatabase::loadTags);
    }

    @Test
    void testFullyRestockTag() throws CustomTraderDatabase.StockUpdateException {
        int num = 6;
        byte b = 1;
        String tag = "myTag";
        Creature trader = factory.createNewCustomTrader(tag);
        CustomTraderDatabase.addStockItemTo(tag, 1, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);

        assertEquals(CustomTraderDatabase.RestockTag.SUCCESS, CustomTraderDatabase.fullyStock(tag));
        assertEquals(num, trader.getInventory().getItemCount());
    }

    @Test
    void testFullyRestockTagEmptyTag() throws CustomTraderDatabase.StockUpdateException {
        int num = 6;
        byte b = 1;
        String tag = "myTag";
        Creature trader = factory.createNewCustomTrader(tag);
        CustomTraderDatabase.addStockItemTo(tag, 1, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, num);

        assertEquals(CustomTraderDatabase.RestockTag.NO_TAG_RECEIVED, CustomTraderDatabase.fullyStock(""));
        assertEquals(0, trader.getInventory().getItemCount());
    }

    @Test
    void testFullyRestockTagNoStock() throws CustomTraderDatabase.StockUpdateException {
        int num = 6;
        byte b = 1;
        String tag = "myTag";
        Creature trader = factory.createNewCustomTrader(tag);

        assertEquals(CustomTraderDatabase.RestockTag.NO_STOCK_FOR_TAG, CustomTraderDatabase.fullyStock(tag));
        assertEquals(0, trader.getInventory().getItemCount());
    }

    @Test
    void testFullyRestockTagNoTraders() throws CustomTraderDatabase.StockUpdateException {
        int num = 6;
        byte b = 1;
        String tag = "myTag";

        assertEquals(CustomTraderDatabase.RestockTag.NO_STOCK_FOR_TAG, CustomTraderDatabase.fullyStock(tag));
    }
}
