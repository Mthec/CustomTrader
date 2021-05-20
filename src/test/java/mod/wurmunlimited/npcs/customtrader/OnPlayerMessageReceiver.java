package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Materials;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OnPlayerMessageReceiver {
    static final String tag = "myTag";
    private static final String wrongTag = "test";
    private static final int maxStock = 5;
    final CustomTraderMod mod = new CustomTraderMod();
    final Creature traderUntagged;
    final Creature traderTagged;
    final Creature traderWrongTag;

    OnPlayerMessageReceiver(Consumer<OnPlayerMessageReceiver> body) throws CustomTraderDatabase.StockUpdateException {
        CustomTraderObjectsFactory factory = (CustomTraderObjectsFactory)WurmObjectsFactory.getCurrent();
        traderUntagged = factory.createNewCustomTrader();
        addStockTo(traderUntagged);
        traderTagged = factory.createNewCustomTrader(tag);
        addStockTo(traderTagged);
        traderWrongTag = factory.createNewCustomTrader(wrongTag);
        addStockTo(traderWrongTag);

        assert traderUntagged.getInventory().getItemCount() == 0;
        assert traderTagged.getInventory().getItemCount() == 0;
        assert traderWrongTag.getInventory().getItemCount() == 0;
    }

    void addStockTo(Creature trader) {
        try {
            CustomTraderDatabase.addStockItemTo(trader, ItemList.book, 1f, 1, Materials.MATERIAL_PAPER, (byte)0, 100, new Enchantment[0], (byte)0, maxStock, 1, 1);
        } catch (CustomTraderDatabase.StockUpdateException e) {
            throw new RuntimeException(e);
        }
    }

    void assertCount(int tagged) {
        assertEquals(0, traderUntagged.getInventory().getItemCount());
        assertEquals(tagged, traderTagged.getInventory().getItemCount());
        assertEquals(0, traderWrongTag.getInventory().getItemCount());
    }
}
