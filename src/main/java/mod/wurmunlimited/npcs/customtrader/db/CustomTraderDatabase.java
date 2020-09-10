package mod.wurmunlimited.npcs.customtrader.db;

import com.wurmonline.server.Constants;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemSpellEffects;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.spells.SpellEffect;
import com.wurmonline.shared.exceptions.WurmServerException;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTemplate;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class CustomTraderDatabase {
    private static final Logger logger = Logger.getLogger(CustomTraderDatabase.class.getName());
    private static String dbString = "";
    public static String tagDumpDbString = "mods/customtrader/tags.db";
    private static boolean created = false;
    private static Clock clock = Clock.systemUTC();

    public interface Execute {

        void run(Connection db) throws SQLException;
    }

    public static class StockUpdateException extends WurmServerException {
        private StockUpdateException(String msg) {
            super(msg);
        }
    }

    public static class FailedToUpdateTagException extends WurmServerException {
        private FailedToUpdateTagException() {
            super("Failed to update tag for trader.");
        }
    }

    private static void init(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS traders (" +
                                                             "id INTEGER UNIQUE," +
                                                             "tag TEXT" +
                                                             ");");
        ps.execute();
        ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS currency_traders (" +
                                           "id INTEGER UNIQUE," +
                                           "currency INTEGER," +
                                           "tag TEXT" +
                                           ");");
        ps.execute();
        ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS trader_stock (" +
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
                                           ");");
        ps.execute();
        ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_stock (" +
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
                                           "UNIQUE(tag, template_id, ql, material, rarity, weight, enchantments) ON CONFLICT REPLACE" +
                                           ");");
        ps.execute();
        ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS last_restock (" +
                                                             "id INTEGER," +
                                                             "stock_hash INTEGER," +
                                                             "last_restock INTEGER," +
                                                             "UNIQUE(id, stock_hash) ON CONFLICT REPLACE" +
                                                             ");");
        ps.execute();

        try (Statement statement = conn.createStatement()) {
            try (ResultSet rs = statement.executeQuery("PRAGMA user_version;")) {
                int version = rs.getInt(1);
                if (version == 0) {
                    conn.prepareStatement("ALTER TABLE trader_stock ADD COLUMN aux INTEGER;").execute();
                    conn.prepareStatement("ALTER TABLE tag_stock ADD COLUMN aux INTEGER;").execute();
                    conn.prepareStatement("PRAGMA user_version = 1;").execute();
                }
            }
        }

        created = true;
    }

    private static void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + "customtrader.db";
            db = DriverManager.getConnection(dbString);
            if (!created) {
                init(db);
            }
            execute.run(db);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (SQLException e1) {
                logger.warning("Could not close connection to database.");
                e1.printStackTrace();
            }
        }
    }

    public static void loadTags() throws SQLException {
        File file = new File(tagDumpDbString);
        if (file.exists()) {
            execute(db -> {
                db.prepareStatement("ATTACH DATABASE '" + tagDumpDbString + "' AS dump;").execute();
                db.prepareStatement("INSERT OR IGNORE INTO main.tag_stock SELECT * FROM dump.tag_stock;").execute();
                db.prepareStatement("DETACH dump;").execute();
            });
        }
    }

    public static void dumpTags() throws SQLException {
        Connection db2 = DriverManager.getConnection("jdbc:sqlite:" + tagDumpDbString);
        db2.close();
        execute(db -> {
            db.prepareStatement("ATTACH DATABASE '" + tagDumpDbString + "' AS dump;").execute();
            db.prepareStatement("DROP TABLE IF EXISTS dump.tag_stock;").execute();
            db.prepareStatement("CREATE TABLE dump.tag_stock AS SELECT * FROM main.tag_stock;").execute();
            db.prepareStatement("DETACH dump;").execute();
        });
    }

    // Custom Trader
    public static void addNew(Creature trader, String tag) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO traders (id, tag) VALUES (?, ?)");
            ps.setLong(1, trader.getWurmId());
            ps.setString(2, tag);

            ps.execute();
        });
    }

    // Currency Trader
    public static void addNew(Creature trader, int currency, String tag) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO currency_traders (id, currency, tag) VALUES (?, ?, ?)");
            ps.setLong(1, trader.getWurmId());
            ps.setInt(2, currency);
            ps.setString(3, tag);

            ps.execute();
        });
    }

    private static void updateLastRestocked(Creature trader, StockItem stockItem) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT OR REPLACE INTO last_restock (id, stock_hash, last_restock) VALUES (?, ?, ?)");
                ps.setLong(1, trader.getWurmId());
                ps.setLong(2, stockItem.hashCode());
                ps.setLong(3, clock.millis());

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when updating last restock.");
            e.printStackTrace();
        }
    }

    private static long getLastRestocked(Creature trader, StockItem stockItem) {
        AtomicReference<Long> lastRestock = new AtomicReference<>(null);

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT last_restock FROM last_restock WHERE id=? AND stock_hash=?");
                ps.setLong(1, trader.getWurmId());
                ps.setLong(2, stockItem.hashCode());
                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst())
                    lastRestock.set(rs.getLong(1));
            });
        } catch (SQLException e) {
            logger.warning("Error when retrieving time of last restock.");
            e.printStackTrace();
        }

        Long last = lastRestock.get();
        if (last != null)
            return last;
        else {
            return 0;
        }
    }

    public static int getCurrencyFor(Creature trader) {
        AtomicInteger currency = new AtomicInteger(-1);

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT currency FROM currency_traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst())
                    currency.set(rs.getInt(1));
                else
                    currency.set(-1);
            });
        } catch (SQLException e) {
            logger.warning("Error when fetching \"currency\" for trader (" + trader.getWurmId() + "), not selecting a currency.");
            e.printStackTrace();
            return -1;
        }

        return currency.get();
    }

    public static void setCurrencyFor(Creature trader, int currency) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE currency_traders SET currency=? WHERE id=?");
                ps.setInt(1, currency);
                ps.setLong(2, trader.getWurmId());

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when settings \"currency\" (" + currency + ") for trader (" + trader.getWurmId() + ")");
            e.printStackTrace();
        }
    }

    public static void updateTag(Creature trader, String tag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                removeItemsFromInventory(trader, getStockFor(trader));

                PreparedStatement ps = db.prepareStatement("UPDATE " +
                                                               (CustomTraderTemplate.isCustomTrader(trader) ? "traders" : "currency_traders")
                                                                + " SET tag=? WHERE id=?");
                ps.setString(1, tag);
                ps.setLong(2, trader.getWurmId());

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Failed to update tag for trader.");
            e.printStackTrace();
            throw new FailedToUpdateTagException();
        }
    }

    public static void deleteTag(String tag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                removeItemsFromInventoryForTag(tag);

                PreparedStatement ps = db.prepareStatement("UPDATE traders SET tag=? WHERE tag=?");
                ps.setString(1, "");
                ps.setString(2, tag);

                ps.execute();

                ps = db.prepareStatement("UPDATE currency_traders SET tag=? WHERE tag=?");
                ps.setString(1, "");
                ps.setString(2, tag);

                ps.execute();

                ps = db.prepareStatement("DELETE FROM tag_stock WHERE tag=?");
                ps.setString(1, tag);

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Failed to delete tag.");
            e.printStackTrace();
            throw new FailedToUpdateTagException();
        }
    }

    public static void renameTag(String from, String to) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE traders SET tag=? WHERE tag=?");
                ps.setString(1, to);
                ps.setString(2, from);

                ps.execute();

                ps = db.prepareStatement("UPDATE currency_traders SET tag=? WHERE tag=?");
                ps.setString(1, to);
                ps.setString(2, from);

                ps.execute();

                ps = db.prepareStatement("UPDATE tag_stock SET tag=? WHERE tag=?");
                ps.setString(1, to);
                ps.setString(2, from);

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Failed to update tag for trader.");
            e.printStackTrace();
            throw new FailedToUpdateTagException();
        }
    }

    public static String getTagFor(Creature trader) {
        AtomicReference<String> tag = new AtomicReference<>(null);

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT tag FROM " +
                                                               (CustomTraderTemplate.isCustomTrader(trader) ? "traders" : "currency_traders")
                                                               + " WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst())
                    tag.set(rs.getString(1));
                else
                    tag.set("");
            });
        } catch (SQLException e) {
            logger.warning("Error when fetching \"tag\" for trader (" + trader.getWurmId() + "), assuming unique trader.");
            e.printStackTrace();
            return "";
        }

        String tagString = tag.get();

        if (tagString != null)
            return tagString;
        else {
            logger.warning("\"tag\" was not set, however no exception occurred.");
            return "";
        }
    }

    public static List<String> getAllTags() {
        List<String> tags = new ArrayList<>();

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT tag FROM traders " +
                                                               "UNION " +
                                                               "SELECT tag FROM currency_traders " +
                                                               "ORDER BY 1 COLLATE NOCASE");
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String tag = rs.getString(1);
                    if (!tag.isEmpty())
                        tags.add(tag);
                }
            });
        } catch (SQLException e) {
            logger.warning("Error when getting all tags.");
            e.printStackTrace();
        }

        return tags;
    }

    public static StockInfo[] getStockFor(Creature trader) {
        return getStockFor(getTagFor(trader), trader);
    }

    private static StockInfo[] getStockFor(String tag, @Nullable Creature trader) {
        try {
            List<StockInfo> stock = new ArrayList<>();

            execute(db -> {
                PreparedStatement ps;

                if (tag.isEmpty()) {
                    assert trader != null;
                    ps = db.prepareStatement("SELECT * FROM trader_stock WHERE trader_id=?");
                    ps.setLong(1, trader.getWurmId());
                } else {
                    ps = db.prepareStatement("SELECT * FROM tag_stock WHERE tag=?");
                    ps.setString(1, tag);
                }

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    stock.add(new StockInfo(new StockItem(
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
                            rs.getInt(11)));
                }
            });

            return stock.toArray(new StockInfo[0]);
        } catch (SQLException e) {
            if (trader == null)
                logger.warning("Could not find stock for " + tag);
            else
                logger.warning("Could not find stock for " + trader.getName());
            e.printStackTrace();
        }

        return new StockInfo[0];
    }

    public static void addStockItemTo(Creature trader, int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, int maxStock, int restockRate, int restockInterval) throws StockUpdateException {
        try {
            String tag = getTagFor(trader);
            if (!tag.isEmpty()) {
                addStockItemTo(tag, templateId, ql, price, material, rarity, weight, enchantments, aux, maxStock, restockRate, restockInterval);
                return;
            }

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT INTO trader_stock VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, trader.getWurmId());
                ps.setInt(2, templateId);
                ps.setFloat(3, ql);
                ps.setInt(4, price);
                ps.setByte(5, material);
                ps.setByte(6, rarity);
                ps.setInt(7, weight);
                ps.setString(8, Enchantment.toSaveString(enchantments));
                ps.setInt(9, maxStock);
                ps.setInt(10, restockRate);
                ps.setInt(11, restockInterval);
                ps.setByte(12, aux);
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not add stock item for " + trader.getName() + " (" + trader.getWurmId() + ").";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    public static void addStockItemTo(String tag, int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, int maxStock, int restockRate, int restockInterval) throws StockUpdateException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT INTO tag_stock VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, tag);
                ps.setInt(2, templateId);
                ps.setFloat(3, ql);
                ps.setInt(4, price);
                ps.setByte(5, material);
                ps.setByte(6, rarity);
                ps.setInt(7, weight);
                ps.setString(8, Enchantment.toSaveString(enchantments));
                ps.setInt(9, maxStock);
                ps.setInt(10, restockRate);
                ps.setInt(11, restockInterval);
                ps.setByte(12, aux);
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not add stock item for the tag " + tag + ".";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    public static void removeStockItemFrom(Creature trader, StockInfo stockInfo) throws StockUpdateException {
        try {
            execute(db -> {
                PreparedStatement ps;
                String tag = getTagFor(trader);
                if (!tag.isEmpty()) {
                    ps = db.prepareStatement("DELETE FROM tag_stock WHERE tag=? AND template_id=? AND ql=? AND material=? AND rarity=? AND weight=? AND enchantments=?");
                    ps.setString(1, tag);

                    removeItemsFromInventoryForTag(tag, stockInfo);
                } else {
                    ps = db.prepareStatement("DELETE FROM trader_stock WHERE trader_id=? AND template_id=? AND ql=? AND material=? AND rarity=? AND weight=? AND enchantments=?");
                    ps.setLong(1, trader.getWurmId());

                    PreparedStatement ps2 = db.prepareStatement("DELETE FROM last_restock WHERE id=? AND stock_hash=?");
                    ps2.setLong(1, trader.getWurmId());
                    ps2.setLong(2, stockInfo.item.hashCode());
                    ps2.execute();

                    removeItemsFromInventory(trader, stockInfo);
                }

                // Unique constraint is (tag/id, template_id, ql, material, rarity, enchantments, weight)
                ps.setInt(2, stockInfo.item.templateId);
                ps.setFloat(3, stockInfo.item.ql);
                ps.setByte(4, stockInfo.item.material);
                ps.setByte(5, stockInfo.item.rarity);
                ps.setFloat(6, stockInfo.item.weight);
                ps.setString(7, Enchantment.toSaveString(stockInfo.item.enchantments));
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not add stock item for " + trader.getName() + " (" + trader.getWurmId() + ").";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    public static void deleteAllStockFor(Creature trader) throws StockUpdateException {
        try {
            execute(db -> {
                removeItemsFromInventory(trader, getStockFor(trader));

                PreparedStatement ps = db.prepareStatement("DELETE FROM trader_stock WHERE trader_id=?");
                ps.setLong(1, trader.getWurmId());
                ps.execute();

                ps = db.prepareStatement("DELETE FROM last_restock WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not remove unique stock for " + trader.getName() + " (" + trader.getWurmId() + ").";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    private static void removeItemsFromInventoryForTag(String tag, StockInfo... stock) {
        if (stock.length == 0)
            stock = getStockFor(tag, null);
        try {
            StockInfo[] finalStock = stock;
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT id FROM traders WHERE tag=? UNION SELECT id FROM currency_traders WHERE tag=?");
                ps.setString(1, tag);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    Creature trader = Creatures.getInstance().getCreatureOrNull(rs.getLong(1));
                    if (trader != null) {
                        removeItemsFromInventory(trader, finalStock);
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("Could not remove items for " + tag + ").");
            e.printStackTrace();
        }
    }

    private static void removeItemsFromInventory(Creature trader, StockInfo... stock) {
        List<Long> toRemove = new ArrayList<>();

        for (Item item : trader.getInventory().getItems()) {
            for (StockInfo stockInfo : stock) {
                if (stockInfo.item.matches(item)) {
                    toRemove.add(item.getWurmId());
                    break;
                }
            }
        }

        for (long wurmId : toRemove) {
            Items.destroyItem(wurmId);
        }
    }

    public static void restock(Creature trader) {
        Map<StockItem, Integer> items = Helper.getInventoryFor(trader);

        for (StockInfo stockInfo : getStockFor(trader)) {
            long timeSinceLastRestock = clock.millis() - getLastRestocked(trader, stockInfo.item);
            if (stockInfo.restockInterval == 0 || timeSinceLastRestock >= stockInfo.restockInterval * TimeConstants.HOUR_MILLIS) {
                int amount = Math.max(0, stockInfo.maxNum - items.getOrDefault(stockInfo.item, 0));

                if (stockInfo.restockRate != 0)
                    amount = Math.min(amount, stockInfo.restockRate);

                restockItem(trader, stockInfo, amount);
            }
        }
    }

    public static void fullyStock(Creature trader) {
        Map<StockItem, Integer> items = Helper.getInventoryFor(trader);
        for (StockInfo stockInfo : CustomTraderDatabase.getStockFor(trader)) {
            int amount = Math.max(0, stockInfo.maxNum - items.getOrDefault(stockInfo.item, 0));
            restockItem(trader, stockInfo, amount);
        }
    }

    private static void restockItem(Creature trader, StockInfo stockInfo, int amount) {
        StockItem stockItem = stockInfo.item;
        Item inventory = trader.getInventory();
        try {
            for (int i = 0; i < amount; ++i) {
                Item item = ItemFactory.createItem(stockItem.templateId, stockItem.ql, stockItem.material, stockItem.rarity, null);
                item.setWeight(stockItem.weight, false);
                item.setPrice(stockItem.price);
                if (stockItem.enchantments.length > 0) {
                    ItemSpellEffects effects = new ItemSpellEffects(item.getWurmId());
                    for (Enchantment enchantment : stockItem.enchantments) {
                        effects.addSpellEffect(new SpellEffect(item.getWurmId(), enchantment.spell.getEnchantment(), enchantment.power, 20000000));
                    }
                }
                item.setAuxData(stockItem.aux);
                inventory.insertItem(item, true);
            }

            updateLastRestocked(trader, stockInfo.item);
        } catch (NoSuchTemplateException | FailedException e) {
            try {
                logger.warning("Error when creating new stock item.  Deleting from database.");
                CustomTraderDatabase.removeStockItemFrom(trader, stockInfo);
            } catch (StockUpdateException e2) {
                logger.warning("Another error occurred when attempting to remove invalid entry from database.");
                e2.printStackTrace();
            }
        }
    }
}
