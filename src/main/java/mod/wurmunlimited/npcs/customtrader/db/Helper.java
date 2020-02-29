package mod.wurmunlimited.npcs.customtrader.db;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;

import java.util.HashMap;
import java.util.Map;

public class Helper {
    static Map<StockItem, Integer> getInventoryFor(Creature trader) {
        Item inventory = trader.getInventory();
        Map<StockItem, Integer> items = new HashMap<>();
        for (Item item : inventory.getItems()) {
            StockItem stock = new StockItem(item.getTemplateId(), item.getQualityLevel(), item.getPrice(), item.getMaterial(), item.getRarity(), Enchantment.parseEnchantments(item));
            items.merge(stock, 1, Integer::sum);
        }

        return items;
    }
}
