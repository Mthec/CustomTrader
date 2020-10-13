package mod.wurmunlimited.npcs.customtrader.db;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Helper {
    private static final Map<Creature, Inventory> inventories = new HashMap<>();

    private static class Inventory {
        private final Map<StockItem, Integer> map = new HashMap<>();
        private int size;

        void add(StockItem stock) {
            map.merge(stock, 1, Integer::sum);
            size += 1;
        }
    }

    static Map<StockItem, Integer> getInventoryFor(Creature trader) {
        Inventory inventory = inventories.get(trader);
        Set<Item> items = trader.getInventory().getItems();
        if (inventory == null || inventory.size != items.size()) {
            inventory = new Inventory();
            inventories.put(trader, inventory);
            for (Item item : items) {
                StockItem stock = new StockItem(item.getTemplateId(), item.getQualityLevel(), item.getPrice(), item.getMaterial(), item.getRarity(), item.getWeightGrams(), Enchantment.parseEnchantments(item), item.getAuxData());
                inventory.add(stock);
            }
        }

        return inventory.map;
    }
}
