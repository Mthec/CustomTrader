package mod.wurmunlimited.npcs.customtrader.db;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelperTests extends CustomTraderTest {
    private Creature trader;
    
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        trader = factory.createNewCustomTrader();
    }

    List<StockItem> createItems() {
        Item itemA = factory.createNewItem(12);
        Item itemB1 = factory.createNewItem(13);
        Item itemB2 = factory.createNewItem(13);
        Item itemC1 = factory.createNewItem(14);
        Item itemC2 = factory.createNewItem(14);
        Item itemC3 = factory.createNewItem(14);
        StockItem stockA = new StockItem(itemA.getTemplateId(), itemA.getQualityLevel(), itemA.getPrice(), itemA.getMaterial(), itemA.getRarity(), itemA.getWeightGrams(), new Enchantment[0], (byte)0, "");
        StockItem stockB = new StockItem(itemB1.getTemplateId(), itemB1.getQualityLevel(), itemB1.getPrice(), itemB1.getMaterial(), itemB1.getRarity(), itemB1.getWeightGrams(), new Enchantment[0], (byte)0, "");
        StockItem stockC = new StockItem(itemC1.getTemplateId(), itemC1.getQualityLevel(), itemC1.getPrice(), itemC1.getMaterial(), itemC1.getRarity(), itemC1.getWeightGrams(), new Enchantment[0], (byte)0, "");
        trader.getInventory().insertItem(itemA);
        trader.getInventory().insertItem(itemB1);
        trader.getInventory().insertItem(itemB2);
        trader.getInventory().insertItem(itemC1);
        trader.getInventory().insertItem(itemC2);
        trader.getInventory().insertItem(itemC3);

        return Arrays.asList(stockA, stockB, stockC);
    }
    
    
    @Test
    void testGetInventoryFor() {
        List<StockItem> items = createItems();
        Map<StockItem, Integer> inventory = Helper.getInventoryFor(trader);
        
        assertEquals(1, (int)inventory.getOrDefault(items.get(0), 0));
        assertEquals(2, (int)inventory.getOrDefault(items.get(1), 0));
        assertEquals(3, (int)inventory.getOrDefault(items.get(2), 0));
    }
    
    @Test
    void testGetInventoryForDoesNotDuplicateEffort() {
        List<StockItem> items = createItems();
        Map<StockItem, Integer> inventory = Helper.getInventoryFor(trader);

        assertEquals(inventory, Helper.getInventoryFor(trader));
    }
}
