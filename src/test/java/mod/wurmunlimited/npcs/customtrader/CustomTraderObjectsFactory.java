package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.FakeShop;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.WurmObjectsFactory;

import static org.mockito.Mockito.mock;

public class CustomTraderObjectsFactory extends WurmObjectsFactory {
    public CustomTraderObjectsFactory() throws Exception {
        super();
        new CustomTraderTemplate().createCreateTemplateBuilder().build();
        new CurrencyTraderTemplate().createCreateTemplateBuilder().build();
    }

    public Creature createNewCustomTrader(VolaTile tile, String name, byte sex, byte kingdom, String tag) {
        Creature trader;
        try {
            trader = CustomTraderTemplate.createNewTrader(tile, 0, name, sex, kingdom, tag);
            creatures.put(trader.getWurmId(), trader);
            trader.createPossessions();
            attachFakeCommunicator(trader);

            return trader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Creature createNewCustomTrader() {
        return createNewCustomTrader("");
    }

    public Creature createNewCustomTrader(String tag) {
        return createNewCustomTrader(mock(VolaTile.class), "Fred" + (creatures.size() + 1), (byte)0, (byte)0, tag);
    }

    public Creature createNewCurrencyTrader(VolaTile tile, String name, byte sex, byte kingdom, int currency, String tag) {
        Creature trader;
        try {
            trader = CurrencyTraderTemplate.createNewTrader(tile, 0, name, sex, kingdom, currency, tag);
            creatures.put(trader.getWurmId(), trader);
            trader.createPossessions();
            attachFakeCommunicator(trader);

            return trader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Creature createNewCurrencyTrader() {
        return createNewCurrencyTrader("");
    }

    public Creature createNewCurrencyTrader(int currency, int multiplier) {
        return createNewCurrencyTrader(mock(VolaTile.class), "Bob" + (creatures.size() + 1), (byte)0, (byte)0, currency, "");
    }

    public Creature createNewCurrencyTrader(String tag) {
        return createNewCurrencyTrader(mock(VolaTile.class), "Bob" + (creatures.size() + 1), (byte)0, (byte)0, ItemList.medallionHota, tag);
    }

    public void addShop(Creature creature, FakeShop shop) {
        shops.put(creature, shop);
    }
}
