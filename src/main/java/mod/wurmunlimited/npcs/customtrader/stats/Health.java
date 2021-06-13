package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

public class Health extends Stat {
    private static final Logger logger = Logger.getLogger(Health.class.getName());
    private static final int maxDamage = 65535;
    private static final StatFactory factory = new StatFactory() {
        private final String name = Health.class.getSimpleName();

        @Override
        public String label() {
            return name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Stat create(float ratio) {
            return new Health(ratio);
        }
    };

    private Health(float ratio) {
        super(ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        int actualAmount = Math.max(1, (int)(amount * ratio));
        int damage = creature.getStatus().damage + actualAmount;
        if (damage >= maxDamage) {
            logger.warning("Could not remove " + actualAmount + " health from " + creature.getName() + ", please report.");
            return false;
        }

        creature.getStatus().modifyWounds(actualAmount);
        return true;
    }

    @Override
    public int creatureHas(Creature creature) {
        return Math.max(0, (int)((maxDamage - 1 - creature.getStatus().damage) * ratio));
    }

    @Override
    public StatFactory getFactory() {
        return factory;
    }

    public static void register() {
        Stat.add(factory);
    }
}
