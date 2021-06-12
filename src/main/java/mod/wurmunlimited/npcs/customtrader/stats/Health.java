package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

public class Health extends Stat {
    private static final Logger logger = Logger.getLogger(Health.class.getName());
    private static final int maxDamage = 65535;

    public Health(float ratio) {
        super(Health.class.getSimpleName(), ratio);
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

    static {
        Stat.stats.put(Health.class.getSimpleName(), Health::new);
    }
}
