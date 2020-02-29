package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemSpellEffects;
import com.wurmonline.server.spells.SpellEffect;
import com.wurmonline.server.spells.Spells;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RestockTests extends CustomTraderTest {
    private final int num = 5;
    private final byte b = 1;

    private void assertItemMatches(Item item, int num, byte b) {
        assertEquals(num, item.getTemplateId());
        assertEquals(num, item.getQualityLevel(), 0.001f);
        assertEquals(num, item.getPrice());
        assertEquals(b, item.getMaterial());
        assertEquals(b, item.getRarity());
    }

    private void assertAllItemsMatch(Creature creature, int num, byte b) {
        for (Item item : creature.getInventory().getItems()) {
            assertItemMatches(item, num, b);
        }
    }

    @Test
    void testNoItemsAddedIfNoStockListed() {
        Creature trader = factory.createNewCustomTrader();
        assert CustomTraderDatabase.getStockFor(trader).length == 0;
        CustomTraderDatabase.restock(trader);

        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testItemAddedIfListedUnique() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.restock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testItemAddedIfListedTag() throws CustomTraderDatabase.StockUpdateException {
        String tag = "test";
        Creature trader = factory.createNewCustomTrader(tag);

        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, new Enchantment[0], num, num, num);
        CustomTraderDatabase.restock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testOnlyMaxNumItemsAllowed() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, num, num);
        Item firstItem = factory.createNewItem(num);
        firstItem.setQualityLevel(num);
        firstItem.setPrice(num);
        firstItem.setMaterial(b);
        firstItem.setRarity(b);
        trader.getInventory().insertItem(firstItem);

        CustomTraderDatabase.restock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testMixedNumberOfItemsToRestock() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], 10, num, 0);
        CustomTraderDatabase.addStockItemTo(trader, num + 1, num, num, b, b, new Enchantment[0], 7, num, 0);

        CustomTraderDatabase.restock(trader);
        CustomTraderDatabase.restock(trader);
        assertEquals(10, trader.getInventory().getItems().stream().filter(i -> i.getTemplateId() == num).count());
        assertEquals(7, trader.getInventory().getItems().stream().filter(i -> i.getTemplateId() == num + 1).count());
    }

    @Test
    void testNoOtherItemsRemoved() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, num, num);
        Item firstItem = factory.createNewItem(num + 1);
        trader.getInventory().insertItem(firstItem);

        CustomTraderDatabase.restock(trader);

        assertEquals(num + 1, trader.getInventory().getItems().size());
        assertTrue(trader.getInventory().getItems().contains(firstItem));
    }

    @Test
    void testItemsRestockedAtProperRate() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, 1, 0);

        for (int i = 1; i <= 5; ++i) {
            CustomTraderDatabase.restock(trader);
            assertEquals(i, trader.getInventory().getItems().size());
        }

        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testItemsNotOverstockedDueToRateNotDividingIntoMaxNumExactly() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        int maxStock = 5;

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], maxStock, 2, 0);

        for (int i = 2; i <= maxStock; i += 2) {
            CustomTraderDatabase.restock(trader);
            assertEquals(i, trader.getInventory().getItems().size());
        }

        CustomTraderDatabase.restock(trader);
        assertEquals(maxStock, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testItemsRestockedImmediatelyIfIntervalIsZero() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, 1, 0);

        for (int i = 1; i <= 5; ++i) {
            CustomTraderDatabase.restock(trader);
            assertEquals(i, trader.getInventory().getItems().size());
        }

        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testItemsRestockedIfIntervalHasPast() throws CustomTraderDatabase.StockUpdateException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        Creature trader = factory.createNewCustomTrader();
        int time = 100;

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, 1, 100);

        CustomTraderDatabase.restock(trader);
        assertEquals(1, trader.getInventory().getItems().size());

        CustomTraderDatabase.restock(trader);
        assertEquals(1, trader.getInventory().getItems().size());

        ReflectionUtil.setPrivateField(null, CustomTraderDatabase.class.getDeclaredField("clock"), Clock.fixed(Instant.now().plusMillis(time * TimeConstants.HOUR_MILLIS), ZoneOffset.UTC));
        CustomTraderDatabase.restock(trader);
        assertEquals(2, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testItemCorrectlyEnchanted() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        Map<String, Enchantment> enchantments = new HashMap<>();
        enchantments.put("Circle of Cunning", new Enchantment(Spells.getSpell(276), 10f));
        enchantments.put("Flaming Aura", new Enchantment(Spells.getSpell(277), 20f));
        enchantments.put("Wind of Ages", new Enchantment(Spells.getSpell(279), 30f));

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, enchantments.values().toArray(new Enchantment[0]), num, num, num);

        CustomTraderDatabase.restock(trader);

        assertEquals(num, trader.getInventory().getItems().size());

        for (Item item : trader.getInventory().getItems()) {
            ItemSpellEffects effects = item.getSpellEffects();
            for (SpellEffect effect : effects.getEffects()) {
                float power = enchantments.get(Spells.getEnchantment(effect.type).name).power;
                assertEquals(power, effect.power);
            }
        }
    }

    @Test
    void testFullyRestockUnique() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, 1, num);
        CustomTraderDatabase.fullyStock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testFullyRestockTag() throws CustomTraderDatabase.StockUpdateException {
        String tag = "testTag";
        Creature trader = factory.createNewCustomTrader(tag);

        CustomTraderDatabase.addStockItemTo(tag, num, num, num, b, b, new Enchantment[0], num, 1, num);
        CustomTraderDatabase.fullyStock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }

    @Test
    void testFullyRestockDoesNotOverstock() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();

        CustomTraderDatabase.addStockItemTo(trader, num, num, num, b, b, new Enchantment[0], num, 1, num);
        CustomTraderDatabase.fullyStock(trader);
        CustomTraderDatabase.fullyStock(trader);

        assertEquals(num, trader.getInventory().getItems().size());
        assertAllItemsMatch(trader, num, b);
    }
}
