package mod.wurmunlimited.npcs.customtrader.stock;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemSpellEffects;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.SpellEffect;
import com.wurmonline.server.spells.Spells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Enchantment {
    private static final Logger logger = Logger.getLogger(Enchantment.class.getName());
    public final Spell spell;
    public final String name;
    public final float power;
    private static final Comparator<Enchantment> comparator = Comparator.comparing((Enchantment e) -> e.spell.number).thenComparing(e -> e.power);

    public Enchantment(Spell spell, float power) {
        this.spell = spell;
        name = spell.getName();
        this.power = power;
    }

    @Override
    public int hashCode() {
        int result = 3;
        result = 31 * result + spell.getEnchantment();
        result = 31 * result + Float.floatToIntBits(power);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Enchantment) {
            Enchantment enchantment = (Enchantment)obj;
            return enchantment.spell == spell &&
                           enchantment.power == power;
        }

        return super.equals(obj);
    }

    public static Enchantment[] parseEnchantments(String code) {
        if (code.isEmpty())
            return new Enchantment[0];

        String[] parts = code.split(",");
        List<Enchantment> enchantments = new ArrayList<>();

        if (parts.length > 0) {
            for (int i = 0; i < parts.length; i += 2) {
                try {
                    int enchant = Integer.parseInt(parts[i]);
                    float power = Float.parseFloat(parts[i + 1]);

                    enchantments.add(new Enchantment(Spells.getSpell(enchant), power));
                } catch (NumberFormatException e) {
                    logger.warning("Error when parsing enchantment, skipping - " + parts[i] + " " + parts[i + 1]);
                }
            }
        }

        enchantments.sort(comparator);
        return enchantments.toArray(new Enchantment[0]);
    }

    public static Enchantment[] parseEnchantments(Item item) {
        List<Enchantment> enchantments = new ArrayList<>();

        ItemSpellEffects effects = item.getSpellEffects();
        if (effects != null) {
            for (SpellEffect effect : effects.getEffects()) {
                enchantments.add(new Enchantment(Spells.getEnchantment(effect.type), effect.power));
            }
        }

        enchantments.sort(comparator);
        return enchantments.toArray(new Enchantment[0]);
    }

    public static String toSaveString(Enchantment[] enchantments) {
        return Arrays.stream(enchantments).map(e -> e.spell.number + "," + e.power).collect(Collectors.joining(","));
    }
}
