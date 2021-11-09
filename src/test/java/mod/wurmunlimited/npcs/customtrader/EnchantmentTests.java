package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.Spells;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnchantmentTests extends CustomTraderTest {
    @Test
    void testParseCode() {
        for (Spell spell : Spells.getSpellsEnchantingItems()) {
            String code = spell.number + "," + 55.0;
            assertEquals(new Enchantment(spell, 55), Enchantment.parseEnchantments(code)[0]);
        }
    }

    @Test
    void testParseItem() throws CustomTraderDatabase.StockUpdateException {
        Creature trader = factory.createNewCustomTrader();
        Map<Spell, Float> allSpells = new HashMap<>();
        float i = 0;
        for (Spell spell : Spells.getSpellsEnchantingItems()) {
            float power = ++i;
            allSpells.put(spell, power);
            CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)0, 1, new Enchantment[] {
                    new Enchantment(spell, power)
            }, (byte)0, "", 1, 1, 0);
        }

        CustomTraderDatabase.restock(trader);

        for (Item item : trader.getInventory().getItems()) {
            Enchantment enchantment = Enchantment.parseEnchantments(item)[0];
            float power = allSpells.get(enchantment.spell);
            assertEquals(power, enchantment.power);
        }
    }

    @Test
    void testToSaveString() {
        for (Spell spell : Spells.getSpellsEnchantingItems()) {
            String code = Enchantment.toSaveString(new Enchantment[] { new Enchantment(spell, 55) });
            assertEquals(spell.number + "," + 55.0, code);
        }
    }

    @Test
    void testToSaveStringMultiple() {
        Spell[] allSpells = Spells.getSpellsEnchantingItems();
        Spell spell1 = allSpells[0];
        Spell spell2 = allSpells[14];
        Spell spell3 = allSpells[27];

        Enchantment[] enchantments = new Enchantment[] {
                new Enchantment(spell1, 1),
                new Enchantment(spell2, 2),
                new Enchantment(spell3, 3)
        };

        String code = Enchantment.toSaveString(enchantments);
        assertEquals(spell1.number + "," + 1.0 + "," +
                             spell2.number + "," + 2.0 + "," +
                             spell3.number + "," + 3.0, code);
    }
}
