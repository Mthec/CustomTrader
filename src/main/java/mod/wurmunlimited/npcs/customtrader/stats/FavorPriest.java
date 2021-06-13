package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

public class FavorPriest extends Favor {
    private static final Logger logger = Logger.getLogger(FavorPriest.class.getName());
    private static final StatFactory factory = new StatFactory() {
        private final String name = FavorPriest.class.getSimpleName();

        @Override
        public String label() {
            return Favor.class.getSimpleName() + " - Priest Only";
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Stat create(float ratio) {
            return new FavorPriest(ratio);
        }
    };

    private FavorPriest(float ratio) {
        super(ratio);
    }

    @Override
    public boolean takeStatFrom(Creature creature, int amount) {
        if (!creature.isPriest()) {
            logger.warning("Tried to take favor from " + creature.getName() + ", who is not a priest, please report.");
            return false;
        }

        return super.takeStatFrom(creature, amount);
    }

    @Override
    public int creatureHas(Creature creature) {
        if (!creature.isPriest())
            return 0;
        return super.creatureHas(creature);
    }

    @Override
    public boolean useBlocked(Creature creature, Creature trader) {
        if (!creature.isPriest()) {
            creature.getCommunicator().sendSafeServerMessage(trader.getName() + " says 'I can only offer my services to priests'.");
            return true;
        }
        return super.useBlocked(creature, trader);
    }

    @Override
    public StatFactory getFactory() {
        return factory;
    }

    public static void register() {
        Stat.add(factory);
    }
}
