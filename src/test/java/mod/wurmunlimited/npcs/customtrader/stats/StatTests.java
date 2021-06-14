package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public abstract class StatTests {
    protected Player player;
    
    protected abstract Stat getStat(float ratio);
    protected abstract void giveStat(Player player, int amount);
    protected void postTest(Player player) {}

    protected Stat create(String name, float ratio) {
        return Objects.requireNonNull(Stat.getFactoryByName(name)).create(ratio);
    }

    @BeforeEach
    void setUp() throws Exception {
        CustomTraderObjectsFactory factory = new CustomTraderObjectsFactory();
        player = factory.createNewPlayer();
    }

    protected void assertAlmostEquals(int expected, int actual) {
        assertTrue(actual < expected + 3, "Expected " + actual + " < " + (expected + 3) + ".");
        assertTrue(actual > expected - 3, "Expected " + actual + " > " + (expected - 3) + ".");
    }

    @Test
    void testCreatureHasStat() {
        Stat stat = getStat(1.0f);

        for (int i = 0; i < 100; i++) {
            giveStat(player, i);
            assertEquals(i, stat.creatureHas(player));
        }
    }

    @Test
    void testRatioAffectsCreatureHasStat() {
        Stat stat = getStat(0.5f);

        for (int i = 0; i < 100; i++) {
            giveStat(player, i);
            assertEquals(Math.max(0, (int)(i * 0.5f)), stat.creatureHas(player));
        }
    }

    @Test
    void testTakeStat() {
        Stat stat = getStat(1.0f);

        for (int i = 1; i < 1000; i++) {
            giveStat(player, i * 2);

            assertTrue(stat.takeStatFrom(player, i));
            assertAlmostEquals(i, stat.creatureHas(player));

            postTest(player);
        }
    }

    @Test
    void testNotEnoughDoesNotTakeStat() {
        Stat stat = getStat(1.0f);

        giveStat(player, 5);

        assertFalse(stat.takeStatFrom(player, 10));
        assertEquals(5, stat.creatureHas(player));

        postTest(player);
    }

    @Test
    void testTakeSmallRatioStat() {
        Stat stat = getStat(0.5f);

        giveStat(player, 6);
        assert stat.creatureHas(player) == 3;

        assertTrue(stat.takeStatFrom(player, 3));
        assertEquals(0, stat.creatureHas(player));

        postTest(player);
    }

    @Test
    void testTakeBigRatioStat() {
        Stat stat = getStat(2.0f);

        giveStat(player, 6);
        assert stat.creatureHas(player) == 12;

        assertTrue(stat.takeStatFrom(player, 3));
        assertEquals(10, stat.creatureHas(player));

        postTest(player);
    }

    @Test
    void testNotTakeRatioWhenRawWouldBeFineStat() {
        Stat stat = getStat(0.5f);

        giveStat(player, 8);
        assert stat.creatureHas(player) == 4;

        assertFalse(stat.takeStatFrom(player, 7));
        assertEquals(4, stat.creatureHas(player));

        postTest(player);
    }

    @Test
    void testTakeMinimumRatioStat() {
        Stat stat = getStat(1000f);

        giveStat(player, 1);

        assertTrue(stat.takeStatFrom(player, 1));
        assertEquals(0, stat.creatureHas(player));

        postTest(player);
    }
}
