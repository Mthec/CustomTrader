package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.FakeShop;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmObjectsFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CustomTraderObjectsFactory extends WurmObjectsFactory {
    private static final List<Character> alphabet = IntStream.rangeClosed('a', 'z').mapToObj(c -> (char)c).collect(Collectors.toList());
    private static final List<Iterator<Character>> chars = new ArrayList<>();

    public CustomTraderObjectsFactory() throws Exception {
        super();
        new CustomTraderTemplate().createCreateTemplateBuilder().build();
        new CurrencyTraderTemplate().createCreateTemplateBuilder().build();
        CustomTraderMod.namePrefix = "Trader";
    }

    private static String randomName(String baseName) {
        if (chars.isEmpty()) {
            chars.add(alphabet.iterator());
        }

        int idx = 0;
        Iterator<Character> toAdd = null;
        List<String> characters = new ArrayList<>();
        while (idx != chars.size()) {
            Iterator<Character> iterator = chars.get(idx++);
            if (iterator.hasNext()) {
                characters.add(iterator.next().toString());
            } else if (idx == chars.size()) {
                chars.add(alphabet.iterator());
            } else {
                chars.set(idx, alphabet.iterator());
            }
        }

        return CustomTraderMod.namePrefix + "_" + baseName + String.join("", characters);
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
        return createNewCustomTrader(Zones.getOrCreateTile(123, 321, true), randomName("Fred"), (byte)0, (byte)0, tag);
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
        return createNewCurrencyTrader(Zones.getOrCreateTile(123, 321, true), randomName("Bob"), (byte)0, (byte)0, currency, "");
    }

    public Creature createNewCurrencyTrader(String tag) {
        return createNewCurrencyTrader(Zones.getOrCreateTile(123, 321, true), randomName("Bob"), (byte)0, (byte)0, ItemList.medallionHota, tag);
    }

    public void addShop(Creature creature, FakeShop shop) {
        shops.put(creature, shop);
    }
}
