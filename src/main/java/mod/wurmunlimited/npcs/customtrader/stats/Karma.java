package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

public class Karma extends Stat {
    private static final Logger logger = Logger.getLogger(Karma.class.getName());
    private static final StatFactory factory = new StatFactory() {
        private final String name = Karma.class.getSimpleName();

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
            return new Karma(ratio);
        }
    };

    private Karma(float ratio) {
        super(ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        int actualAmount = Math.max(1, (int)(amount * ratio));
        int newKarma = creature.getKarma() - actualAmount;

        if (newKarma < 0) {
            logger.warning("Could not remove " + actualAmount + " karma from " + creature.getName() + ", please report.");
            return false;
        }

        creature.setKarma(newKarma);
        return true;
    }

    @Override
    public int creatureHas(Creature creature) {
        return Math.max(0, (int)(creature.getKarma() * ratio));
    }

    @Override
    public StatFactory getFactory() {
        return factory;
    }

    public static void register() {
        Stat.add(factory);
    }
}
