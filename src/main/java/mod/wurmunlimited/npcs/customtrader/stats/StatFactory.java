package mod.wurmunlimited.npcs.customtrader.stats;

public interface StatFactory {
    String label();
    String name();
    Stat create(float ratio);
}
