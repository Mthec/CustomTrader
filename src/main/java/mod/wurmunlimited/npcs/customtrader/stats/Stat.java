package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class Stat {
    protected static final Map<String, StatFactory> stats = new HashMap<>();
    public final String name;
    public final float ratio;

    protected Stat(String name, float ratio) {
        this.name = name;
        this.ratio = ratio;
    }

    public abstract boolean takeStatFrom(Creature creature, int amount);

    public abstract int creatureHas(Creature creature);

    public static @Nullable Stat create(String name, float ratio) {
        return stats.get(name).create(ratio);
    }

    public static String[] getAll() {
        return stats.keySet().toArray(new String[0]);
    }
}
