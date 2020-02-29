package com.wurmonline.server.questions;

import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.Spells;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Enchantments implements Iterable<Enchantment> {
    static String allEnchantmentsString;
    static Spell[] allEnchantments;
    private final Map<Spell, Float> enchantments = new HashMap<>();
    private final List<Spell> ordering = new ArrayList<>();

    static {
        allEnchantments = Spells.getSpellsEnchantingItems();
        Arrays.sort(allEnchantments, Comparator.comparing(Spell::getName));
        allEnchantmentsString = Arrays.stream(allEnchantments).map(Spell::getName).collect(Collectors.joining(","));
    }

    float getPower(Spell spell) {
        return enchantments.getOrDefault(spell, 0f);
    }

    void setPower(Spell spell, float power) {
        enchantments.put(spell, power);

        if (!ordering.contains(spell)) {
            ordering.add(spell);
        }
    }

    void remove(Spell spell) {
        enchantments.remove(spell);
        ordering.remove(spell);
    }

    @NotNull
    @Override
    public Iterator<Enchantment> iterator() {
        return new Iterator<Enchantment>() {
            private Iterator<Spell> order = ordering.iterator();

            @Override
            public boolean hasNext() {
                return order.hasNext();
            }

            @Override
            public Enchantment next() {
                Spell spell = order.next();
                return new Enchantment(spell, enchantments.get(spell));
            }
        };
    }

    List<Enchantment> toList() {
        List<Enchantment> list = new ArrayList<>();
        iterator().forEachRemaining(list::add);
        return list;
    }
}
