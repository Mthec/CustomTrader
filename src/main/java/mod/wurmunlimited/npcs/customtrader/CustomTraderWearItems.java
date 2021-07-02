package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.WearItems;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.StockItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomTraderWearItems implements WearItems {
    private final List<Item> stockItems = new ArrayList<>();

    @Override
    public boolean isApplicableCreature(Creature creature) {
        return CustomTraderTemplate.isCustomTrader(creature) || CustomTraderMod.isOtherTrader(creature);
    }

    @Override
    public void beforeWearing(Creature creature) {
        Set<Item> inventory = creature.getInventory().getItems();
        Set<StockItem> stock = Arrays.stream(CustomTraderDatabase.getStockFor(creature)).map(it -> it.item).collect(Collectors.toSet());
        for (Item item : creature.getInventory().getItemsAsArray()) {
            if (stock.stream().anyMatch(it -> it.matches(item))) {
                inventory.remove(item);
                stockItems.add(item);
            }
        }
    }

    @Override
    public void afterWearing(Creature creature) {
        creature.getInventory().getItems().addAll(stockItems);

        stockItems.clear();
    }
}
