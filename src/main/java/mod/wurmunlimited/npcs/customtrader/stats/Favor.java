package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.io.IOException;
import java.util.logging.Logger;

public class Favor extends Stat {
    private static final Logger logger = Logger.getLogger(Favor.class.getName());
    private static final StatFactory factory = new StatFactory() {
        private final String name = Favor.class.getSimpleName();

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
            return new Favor(ratio);
        }
    };
    private static final float minimumFavor = 0.0001f;

    protected Favor(float ratio) {
        super(ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        float actualAmount = Math.max(minimumFavor, amount / ratio);
        float newFavor = creature.getFavor() - actualAmount;

        if (newFavor < 0) {
            logger.warning("Could not remove " + actualAmount + " favor from " + creature.getName() + " as it would be too low, please report.");
            return false;
        }

        try {
            creature.depleteFavor(actualAmount, false);
        } catch (IOException e) {
            logger.warning("Could not remove " + actualAmount + " favor from " + creature.getName() + ", please report.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public int creatureHas(Creature creature) {
        return Math.max(0, (int)(creature.getFavor() * ratio));
    }

    @Override
    public StatFactory getFactory() {
        return factory;
    }

    public static void register() {
        Stat.add(factory);
    }
}
