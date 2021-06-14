package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Stat {
    private static final List<StatFactory> stats = new ArrayList<>();
    private static final Map<String, StatFactory> statsByName = new HashMap<>();
    public final String name;
    public final float ratio;

    protected Stat(float ratio) {
        this.name = getFactory().name();
        this.ratio = ratio;
    }

    public abstract boolean takeStatFrom(Creature creature, int amount);

    public abstract int creatureHas(Creature creature);

    public abstract StatFactory getFactory();

    public String label() {
        return getFactory().label();
    }

    public boolean useBlocked(Creature creature, Creature trader) {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Stat) {
            Stat stat = (Stat)other;
            return stat.name.equals(name) && stat.ratio == ratio;
        }

        return false;
    }

    public static List<StatFactory> getAll() {
        return stats;
    }

    protected static void add(StatFactory factory) {
        stats.add(factory);
    }

    public static @Nullable StatFactory getFactoryByName(String name) {
        if (statsByName.size() != stats.size()) {
            statsByName.clear();
            for (StatFactory factory : stats) {
                statsByName.put(factory.name(), factory);
            }
        }

        return statsByName.get(name);
    }
}
