package mod.wurmunlimited.npcs.customtrader.db;

import com.wurmonline.server.Constants;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.*;
import com.wurmonline.server.spells.SpellEffect;
import com.wurmonline.shared.exceptions.WurmServerException;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.*;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stats.StatFactory;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class CustomTraderDatabase {
    private static final Logger logger = Logger.getLogger(CustomTraderDatabase.class.getName());
    private static String dbString = "";
    public static String tagDumpDbString = "mods/customtrader/tags.db";
    private static boolean created = false;
    public static Clock clock = Clock.systemUTC();
    private static final Map<Creature, String> tags = new HashMap<>();
    private static final Map<Creature, Currency> currencies = new HashMap<>();

    public interface Execute {
        void run(Connection db) throws SQLException;
    }

    public enum RestockTag {
        SUCCESS, NO_TRADERS_FOUND, NO_TAG_RECEIVED, NO_STOCK_FOR_TAG
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

    private static void init() throws SQLException {
        int version;
        try (Connection conn = DriverManager.getConnection(dbString)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS traders (" +
                                                                 "id INTEGER UNIQUE," +
                                                                 "tag TEXT" +
                                                                 ");").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS currency_traders (" +
                                               "id INTEGER UNIQUE," +
                                               "currency INTEGER," +
                                               "tag TEXT" +
                                               ");").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS stat_traders (" +
                                               "id INTEGER UNIQUE," +
                                               "stat TEXT," +
                                               "ratio REAL," +
                                               "tag TEXT" +
                                               ");").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS trader_stock (" +
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
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_stock (" +
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
                                               ");").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS last_restock (" +
                                               "id INTEGER," +
                                               "stock_hash INTEGER," +
                                               "last_restock INTEGER," +
                                               "UNIQUE(id, stock_hash) ON CONFLICT REPLACE" +
                                               ");").execute();

            ResultSet rs = conn.prepareStatement("PRAGMA user_version;").executeQuery();
            version = rs.getInt(1);
            if (version == 0) {
                conn.prepareStatement("ALTER TABLE trader_stock ADD COLUMN aux INTEGER;").execute();
                conn.prepareStatement("ALTER TABLE tag_stock ADD COLUMN aux INTEGER;").execute();
                conn.prepareStatement("PRAGMA user_version = 1;").execute();
                version = 1;
            }

            if (version == 1) {
                conn.prepareStatement("ALTER TABLE currency_traders ADD COLUMN minimum_ql REAL NOT NULL DEFAULT -1;").execute();
                conn.prepareStatement("ALTER TABLE currency_traders ADD COLUMN exact_ql REAL NOT NULL DEFAULT -1;").execute();
                conn.prepareStatement("ALTER TABLE currency_traders ADD COLUMN material INTEGER NOT NULL DEFAULT -1;").execute();
                conn.prepareStatement("ALTER TABLE currency_traders ADD COLUMN rarity INTEGER NOT NULL DEFAULT -1;").execute();
                conn.prepareStatement("ALTER TABLE currency_traders ADD COLUMN weight INTEGER NOT NULL DEFAULT 1;").execute();
                conn.prepareStatement("PRAGMA user_version = 2;").execute();
                version = 2;
            }
        }

        if (version == 2) {
            try (Connection conn = DriverManager.getConnection(dbString)) {
                conn.setAutoCommit(false);

                conn.prepareStatement("CREATE TABLE IF NOT EXISTS trader_stock_copy (" +
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
                                              "aux INTEGER" +
                                              ");").execute();
                conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_stock_copy (" +
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
                                              "aux INTEGER" +
                                              ");").execute();

                conn.prepareStatement("INSERT INTO trader_stock_copy SELECT * FROM trader_stock;").execute();
                conn.prepareStatement("INSERT INTO tag_stock_copy SELECT * FROM tag_stock;").execute();

                conn.commit();
            }

            try (Connection conn = DriverManager.getConnection(dbString)) {
                conn.setAutoCommit(false);
                conn.prepareStatement("DROP TABLE trader_stock;").execute();
                conn.prepareStatement("DROP TABLE tag_stock;").execute();

                conn.prepareStatement("CREATE TABLE IF NOT EXISTS trader_stock (" +
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
                                              "aux INTEGER," +
                                              "inscription TEXT NOT NULL DEFAULT ''," +
                                              "UNIQUE(trader_id, template_id, ql, material, rarity, weight, enchantments, aux, inscription) ON CONFLICT REPLACE" +
                                              ");").execute();
                conn.prepareStatement("CREATE TABLE IF NOT EXISTS tag_stock (" +
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
                                              "inscription TEXT NOT NULL DEFAULT ''," +
                                              "UNIQUE(tag, template_id, ql, material, rarity, weight, enchantments, aux, inscription) ON CONFLICT REPLACE" +
                                              ");").execute();

                conn.prepareStatement("INSERT INTO trader_stock (trader_id, template_id, ql, price, material, rarity, weight, enchantments, max_num, restock_rate, restock_interval, aux) " +
                                              "SELECT * FROM trader_stock_copy;").execute();
                conn.prepareStatement("INSERT INTO tag_stock (tag, template_id, ql, price, material, rarity, weight, enchantments, max_num, restock_rate, restock_interval, aux) " +
                                              "SELECT * FROM tag_stock_copy;").execute();

                conn.prepareStatement("DROP TABLE trader_stock_copy;").execute();
                conn.prepareStatement("DROP TABLE tag_stock_copy;").execute();

                conn.prepareStatement("PRAGMA user_version = 3;").execute();
                conn.setAutoCommit(true);
            }
        }

        created = true;
    }

    private static void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + CustomTraderMod.dbName;
            if (!created) {
                init();
            }
            db = DriverManager.getConnection(dbString);
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

    @SuppressWarnings({"SqlResolve", "SqlInsertValues"})
    public static void loadTags() throws SQLException {
        File file = new File(tagDumpDbString);
        if (file.exists()) {
            execute(db -> {
                db.prepareStatement("ATTACH DATABASE '" + tagDumpDbString + "' AS dump;").execute();
                db.prepareStatement("INSERT OR IGNORE INTO main.tag_stock SELECT * FROM dump.tag_stock;").execute();
                db.prepareStatement("DETACH dump;").execute();
                tags.clear();
            });
        }
    }

    @SuppressWarnings("SqlResolve")
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
    public static void addNew(@NotNull Creature trader, @NotNull Currency currency, String tag) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO currency_traders (id, currency, tag, minimum_ql, exact_ql, material, rarity, weight) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, trader.getWurmId());
            ps.setInt(2, currency.templateId);
            ps.setString(3, tag);
            ps.setFloat(4, currency.minQL);
            ps.setFloat(5, currency.exactQL);
            ps.setByte(6, currency.material);
            ps.setByte(7, currency.rarity);
            ps.setBoolean(8, currency.onlyFullWeight);

            ps.execute();

            currencies.put(trader, currency);
        });
    }

    // Stat Trader
    public static void addNew(Creature trader, Stat stat, String tag) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR IGNORE INTO stat_traders (id, stat, ratio, tag) VALUES (?, ?, ?, ?)");
            ps.setLong(1, trader.getWurmId());
            ps.setString(2, stat.name);
            ps.setFloat(3, stat.ratio);
            ps.setString(4, tag);

            ps.execute();
        });
    }

    public static void deleteTrader(Creature trader) {
        String table;
        if (CustomTraderTemplate.isCustomTrader(trader)) {
            table = "traders";
        } else if (CurrencyTraderTemplate.isCurrencyTrader(trader)) {
            table = "currency_traders";
        } else if (StatTraderTemplate.is(trader)) {
            table = "stat_traders";
        } else {
            return;
        }

        try {
            execute(db -> {
                //noinspection SqlResolve
                PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?;");
                ps.setLong(1, trader.getWurmId());

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when deleting trader from " + table + ".");
            e.printStackTrace();
        }

        tags.remove(trader);
        currencies.remove(trader);
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

    private static String getTraderTable(Creature trader) {
        if (CurrencyTraderTemplate.isCurrencyTrader(trader)) {
            return "currency_traders";
        } else if (StatTraderTemplate.is(trader)) {
            return "stat_traders";
        } else {
            return "traders";
        }
    }

    public static @Nullable Currency getCurrencyFor(Creature trader) {
        Currency possibleCurrency = currencies.get(trader);
        if (possibleCurrency != null) {
            return possibleCurrency;
        }

        AtomicReference<Currency> currency = new AtomicReference<>(null);

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT currency, minimum_ql, exact_ql, material, rarity, weight FROM currency_traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst()) {
                    try {
                        currency.set(new Currency(
                                rs.getInt(1),
                                rs.getFloat(2),
                                rs.getFloat(3),
                                rs.getByte(4),
                                rs.getByte(5),
                                rs.getBoolean(6)
                                ));
                    } catch (NoSuchTemplateException e) {
                        logger.warning("Error when fetching \"currency\" (" + rs.getInt(1) + ") for trader (" + trader.getWurmId() + "), not selecting a currency.");
                        e.printStackTrace();
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("Error when fetching \"currency\" for trader (" + trader.getWurmId() + "), not selecting a currency.");
            e.printStackTrace();
            return null;
        }

        Currency curr = currency.get();
        if (curr != null) {
            currencies.put(trader, curr);
        }
        return curr;
    }

    public static void setCurrencyFor(Creature trader, @NotNull ItemTemplate newCurrency, byte newMaterial, @NotNull Currency oldCurrency) {
        setCurrencyFor(trader, new Currency(newCurrency, oldCurrency.minQL, oldCurrency.exactQL, newMaterial, oldCurrency.rarity, oldCurrency.onlyFullWeight));
    }

    public static void setCurrencyFor(Creature trader, @NotNull ItemTemplate newCurrency, @NotNull Currency oldCurrency) {
        setCurrencyFor(trader, new Currency(newCurrency, oldCurrency.minQL, oldCurrency.exactQL, oldCurrency.material, oldCurrency.rarity, oldCurrency.onlyFullWeight));
    }

    public static void setCurrencyFor(Creature trader, Currency currency) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE currency_traders SET currency=?, minimum_ql=?, exact_ql=?, material=?, rarity=?, weight=? WHERE id=?;");
                ps.setInt(1, currency.templateId);
                ps.setFloat(2, currency.minQL);
                ps.setFloat(3, currency.exactQL);
                ps.setByte(4, currency.material);
                ps.setByte(5, currency.rarity);
                ps.setBoolean(6, currency.onlyFullWeight);
                ps.setLong(7, trader.getWurmId());

                ps.execute();

                currencies.put(trader, currency);
            });
        } catch (SQLException e) {
            logger.warning("Error when setting \"currency\" (" + currency.toString() + ") for trader (" + trader.getWurmId() + ").");
            e.printStackTrace();
        }
    }

    public static @Nullable Stat getStatFor(Creature trader) {
        AtomicReference<String> stat = new AtomicReference<>(null);
        AtomicReference<Float> ratioValue = new AtomicReference<>(null);

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT stat, ratio FROM stat_traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                if (rs.isBeforeFirst()) {
                    stat.set(rs.getString(1));
                    ratioValue.set(rs.getFloat(2));
                }
            });
        } catch (SQLException e) {
            logger.warning("Error when fetching \"stat\" for trader (" + trader.getWurmId() + "), not selecting a stat.");
            e.printStackTrace();
            return null;
        }

        String name = stat.get();
        Float ratio = ratioValue.get();
        if (name == null || ratio == null) {
            logger.warning("Stat name (" + name + ") and/or ratio (" + ratio + ") were null.");
            return null;
        }

        StatFactory factory = Stat.getFactoryByName(name);
        if (factory == null) {
            logger.warning("StatFactory was null (" + name + ").");
            return null;
        }
        return factory.create(ratio);
    }

    public static void setStatFor(Creature trader, Stat stat) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE stat_traders SET stat=?, ratio=? WHERE id=?");
                ps.setString(1, stat.name);
                ps.setFloat(2, stat.ratio);
                ps.setLong(3, trader.getWurmId());

                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when setting \"stat\" (" + stat.name + ") for trader (" + trader.getWurmId() + ")");
            e.printStackTrace();
        }
    }

    public static void updateTag(Creature trader, String tag) throws FailedToUpdateTagException {
        try {
            execute(db -> {
                removeItemsFromInventory(trader, getStockFor(trader));

                //noinspection SqlResolve
                PreparedStatement ps = db.prepareStatement("UPDATE " + getTraderTable(trader) + " SET tag=? WHERE id=?");
                ps.setString(1, tag);
                ps.setLong(2, trader.getWurmId());

                ps.execute();

                tags.put(trader, tag);
            });
        } catch (SQLException e) {
            logger.warning("Failed to update tag for " + trader.getName() + ".");
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

                ps = db.prepareStatement("UPDATE stat_traders SET tag=? WHERE tag=?");
                ps.setString(1, "");
                ps.setString(2, tag);
                ps.execute();

                ps = db.prepareStatement("DELETE FROM tag_stock WHERE tag=?");
                ps.setString(1, tag);
                ps.execute();

                tags.entrySet().removeIf(e -> e.getValue().equals(tag));
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

                ps = db.prepareStatement("UPDATE stat_traders SET tag=? WHERE tag=?");
                ps.setString(1, to);
                ps.setString(2, from);
                ps.execute();

                ps = db.prepareStatement("UPDATE tag_stock SET tag=? WHERE tag=?");
                ps.setString(1, to);
                ps.setString(2, from);
                ps.execute();

                tags.entrySet().removeIf(e -> e.getValue().equals(from));
            });
        } catch (SQLException e) {
            logger.warning("Failed to update tag (" + from + " -> " + to + ") for trader.");
            e.printStackTrace();
            throw new FailedToUpdateTagException();
        }
    }

    public static String getTagFor(Creature trader) {
        String possibleTag = tags.get(trader);
        if (possibleTag != null) {
            return possibleTag;
        }

        AtomicReference<String> tag = new AtomicReference<>(null);

        try {
            execute(db -> {
                //noinspection SqlResolve
                PreparedStatement ps = db.prepareStatement("SELECT tag FROM " +
                                                               getTraderTable(trader)
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
        tags.put(trader, tagString);

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
                                                               "UNION " +
                                                               "SELECT tag FROM stat_traders " +
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

    private static List<Creature> getTradersWithTag(String tag) {
        List<Creature> traders = new ArrayList<>();

        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT id FROM traders WHERE tag=? " +
                                                               "UNION " +
                                                               "SELECT id FROM currency_traders WHERE tag=? " +
                                                               "UNION " +
                                                               "SELECT id FROM stat_traders WHERE tag=? " +
                                                               "ORDER BY 1 COLLATE NOCASE");
                ps.setString(1, tag);
                ps.setString(2, tag);
                ps.setString(3, tag);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    long id = rs.getLong(1);
                    try {
                        traders.add(Creatures.getInstance().getCreature(id));
                    } catch (NoSuchCreatureException e) {
                        logger.warning("Creature (" + id + ") not found when getting traders with tag (" + tag + ").");
                        e.printStackTrace();
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("Error when getting traders with tag (" + tag + ").");
            e.printStackTrace();
        }

        return traders;
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
                            rs.getByte(12),
                            rs.getString(13)),
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

    public static void addStockItemTo(Creature trader, int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, String inscription, int maxStock, int restockRate, int restockInterval) throws StockUpdateException {
        try {
            String tag = getTagFor(trader);
            if (!tag.isEmpty()) {
                addStockItemTo(tag, templateId, ql, price, material, rarity, weight, enchantments, aux, inscription, maxStock, restockRate, restockInterval);
                return;
            }

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT INTO trader_stock VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, trader.getWurmId());
                setStockValues(templateId, ql, price, material, rarity, weight, enchantments, aux, inscription, maxStock, restockRate, restockInterval, ps);
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not add stock item for " + trader.getName() + " (" + trader.getWurmId() + ").";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    public static void addStockItemTo(String tag, int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, String inscription, int maxStock, int restockRate, int restockInterval) throws StockUpdateException {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT INTO tag_stock VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, tag);
                setStockValues(templateId, ql, price, material, rarity, weight, enchantments, aux, inscription, maxStock, restockRate, restockInterval, ps);
                ps.execute();
            });
        } catch (SQLException e) {
            String msg = "Could not add stock item for the tag " + tag + ".";
            logger.warning(msg);
            e.printStackTrace();
            throw new StockUpdateException(msg);
        }
    }

    private static void setStockValues(int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, String inscription, int maxStock, int restockRate, int restockInterval, PreparedStatement ps) throws SQLException {
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
        ps.setString(13, inscription);
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
            if (stockInfo.restockInterval == 0 || (clock.millis() - getLastRestocked(trader, stockInfo.item)) >= stockInfo.restockInterval * TimeConstants.HOUR_MILLIS) {
                int amount = Math.max(0, stockInfo.maxNum - items.getOrDefault(stockInfo.item, 0));

                if (stockInfo.restockRate != 0)
                    amount = Math.min(amount, stockInfo.restockRate);

                if (amount != 0)
                    restockItem(trader, stockInfo, amount);
            }
        }
    }

    public static RestockTag fullyStock(String tag) {
        if (tag == null || tag.isEmpty()) {
            return RestockTag.NO_TAG_RECEIVED;
        }

        StockInfo[] stock = getStockFor(tag, null);
        if (stock.length == 0) {
            return RestockTag.NO_STOCK_FOR_TAG;
        }

        List<Creature> traders = getTradersWithTag(tag);
        if (traders.isEmpty()) {
            return RestockTag.NO_TRADERS_FOUND;
        }

        for (Creature trader : traders) {
            fullyStock(trader);
        }

        return RestockTag.SUCCESS;
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
                if (!stockItem.inscription.isEmpty() && item.canHaveInscription()) {
                    item.setInscription(stockItem.inscription, "");

                    if (stockItem.aux == 1) {
                        Recipe recipe = Objects.requireNonNull(item.getInscription()).getRecipe();
                        if (recipe != null) {
                            item.setName("\"" + recipe.getName() + "\"", true);
                            item.setRarity(recipe.getLootableRarity());
                        }
                    }
                }
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
