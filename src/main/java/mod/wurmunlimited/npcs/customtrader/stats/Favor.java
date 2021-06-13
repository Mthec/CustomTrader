package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.io.IOException;
import java.util.logging.Logger;

public class Favor extends Stat {
    private static final Logger logger = Logger.getLogger(Favor.class.getName());
    private static final float minimumFavor = 0.0001f;

    private Favor(float ratio) {
        super(Favor.class.getSimpleName(), ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        float actualAmount = Math.max(minimumFavor, amount * ratio);
        float newFavor = creature.getFavor() - actualAmount;

        if (newFavor < 0) {
            logger.warning("Could not remove " + actualAmount + " favor from " + creature.getName() + ", please report.");
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

    public static void register() {
        Stat.add(Favor.class.getSimpleName(), Favor::new);
    }
}
