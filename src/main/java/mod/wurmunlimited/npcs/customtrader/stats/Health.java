package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

public class Health extends Stat {
    private static final Logger logger = Logger.getLogger(Health.class.getName());
    private static final int maxDamage = 65534;
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
    private static final byte woundType = 9;
    private static final byte location = 2;

    private Health(float ratio) {
        super(ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        int actualAmount = Math.max(1, (int)(amount / ratio));
        int damage = creature.getStatus().damage + actualAmount;
        if (damage > maxDamage) {
            logger.warning("Could not remove " + actualAmount + " health from " + creature.getName() + ", please report.");
            return false;
        }

        // To stop modified severity causing death.
        float moddedAmount = (float)Math.max(1, actualAmount / ((120f - creature.getStrengthSkill()) / 100f));

        creature.addWoundOfType(null, woundType, location, false, 1.0f, false, Math.floor(moddedAmount), 0.0f, 0.0f, true, false);

        Wound wound = creature.getBody().getWounds().getWoundTypeAtLocation(location, woundType);
        if (wound == null) {
            logger.warning("Could not modify wound (" + actualAmount + ") for " + creature.getName() + ", please report.");
            return true;
        }

        if (creature.getStatus().damage < damage) {
            wound.modifySeverity(1);
        }

        creature.getCurrentTile().broadCast("The trader drains life-force from " + creature.getName() + ".");
        return true;
    }

    @Override
    public int creatureHas(Creature creature) {
        return Math.max(0, (int)((maxDamage - creature.getStatus().damage) * ratio));
    }

    @Override
    public StatFactory getFactory() {
        return factory;
    }

    public static void register() {
        Stat.add(factory);
    }
}
