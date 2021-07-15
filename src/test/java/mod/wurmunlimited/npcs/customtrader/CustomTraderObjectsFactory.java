package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.npcs.customtrader.stats.Karma;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;

import java.util.Objects;

public class CustomTraderObjectsFactory extends WurmObjectsFactory {
    private static boolean createdTemplates = false;

    public CustomTraderObjectsFactory() throws Exception {
        super();
        if (!createdTemplates) {
            new CustomTraderTemplate().createCreateTemplateBuilder().build();
            new CurrencyTraderTemplate().createCreateTemplateBuilder().build();
            new StatTraderTemplate().createCreateTemplateBuilder().build();
            createdTemplates = true;
        }
        CustomTraderMod.namePrefix = "Trader";
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
        return createNewCustomTrader(Zones.getOrCreateTile(123, 321, true), randomName("Fred"), (byte)0, Kingdom.KINGDOM_FREEDOM, tag);
    }

    public Creature createNewCurrencyTrader(VolaTile tile, String name, byte sex, byte kingdom, int currency, String tag) {
        try {
            return createNewCurrencyTrader(tile, name, sex, kingdom, new Currency(ItemTemplateFactory.getInstance().getTemplate(currency)), tag);
        } catch (NoSuchTemplateException e) {
            throw new RuntimeException(e);
        }
    }

    public Creature createNewCurrencyTrader(VolaTile tile, String name, byte sex, byte kingdom, Currency currency, String tag) {
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

    public Creature createNewCurrencyTrader(Currency currency) {
        return createNewCurrencyTrader(Zones.getOrCreateTile(123, 321, true), randomName("Bob"), (byte)0, Kingdom.KINGDOM_FREEDOM, currency, "");
    }

    public Creature createNewCurrencyTrader(int currency) {
        return createNewCurrencyTrader(Zones.getOrCreateTile(123, 321, true), randomName("Bob"), (byte)0, Kingdom.KINGDOM_FREEDOM, currency, "");
    }

    public Creature createNewCurrencyTrader(String tag) {
        return createNewCurrencyTrader(Zones.getOrCreateTile(123, 321, true), randomName("Bob"), (byte)0, Kingdom.KINGDOM_FREEDOM, ItemList.medallionHota, tag);
    }

    public Creature createNewStatTrader() {
        return createNewStatTrader(Objects.requireNonNull(Stat.getFactoryByName(Karma.class.getSimpleName())).create(1.0f));
    }

    public Creature createNewStatTrader(Stat stat) {
        return createNewStatTrader(Zones.getOrCreateTile(123, 321, true), randomName("George"), (byte)0, Kingdom.KINGDOM_FREEDOM, stat, "");
    }

    public Creature createNewStatTrader(VolaTile tile, String name, byte sex, byte kingdom, Stat stat, String tag) {
        Creature trader;
        try {
            trader = StatTraderTemplate.createNewTrader(tile, 0, name, sex, kingdom, stat, tag);
            creatures.put(trader.getWurmId(), trader);
            trader.createPossessions();
            attachFakeCommunicator(trader);

            return trader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Item createNewItem(Currency currency) {
        Item item = createNewItem(currency.templateId);
        if (currency.exactQL > 0) {
            item.setQualityLevel(currency.exactQL);
        } else if (currency.minQL > 0) {
            item.setQualityLevel(currency.minQL);
        }
        if (currency.material >= 0) {
            item.setMaterial(currency.material);
        }
        if (currency.rarity >= 0) {
            item.setRarity(currency.rarity);
        }
        return item;
    }
}
