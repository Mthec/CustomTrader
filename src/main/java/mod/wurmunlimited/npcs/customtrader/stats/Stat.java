package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class Stat {
    private static final Map<String, StatFactory> stats = new HashMap<>();
    public final String name;
    public final float ratio;

    protected Stat(String name, float ratio) {
        this.name = name;
        this.ratio = ratio;
    }

    public abstract boolean takeStatFrom(Creature creature, int amount);

    public abstract int creatureHas(Creature creature);

    @Override
    public boolean equals(Object other) {
        if (other instanceof Stat) {
            Stat stat = (Stat)other;
            return stat.name.equals(name) && stat.ratio == ratio;
        }

        return false;
    }

    public static @Nullable Stat create(String name, float ratio) {
        return stats.get(name).create(ratio);
    }

    public static String[] getAll() {
        return stats.keySet().stream().sorted().toArray(String[]::new);
    }

    protected static void add(String name, StatFactory factory) {
        stats.put(name, factory);
    }
}
