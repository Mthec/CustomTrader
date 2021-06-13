package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class StatTests {
    private Player player;
    
    protected abstract Stat getStat(float ratio);
    protected abstract void giveStat(Player player, int amount);
    protected abstract int getHas(Player player);
    protected void postTest(Player player) {}

    @BeforeEach
    void setUp() throws Exception {
        CustomTraderObjectsFactory factory = new CustomTraderObjectsFactory();
        player = factory.createNewPlayer();
    }

    @Test
    void testCreatureHasStat() {
        Stat stat = getStat(1.0f);

        for (int i = 0; i < 10; i++) {
            giveStat(player, i);
            assertEquals(i, stat.creatureHas(player));
        }
    }

    @Test
    void testRatioAffectsCreatureHasStat() {
        Stat stat = getStat(0.5f);

        for (int i = 0; i < 10; i++) {
            giveStat(player, i);
            assertEquals(Math.max(0, (int)(i * 0.5f)), stat.creatureHas(player));
        }
    }

    @Test
    void testTakeStat() {
        Stat stat = getStat(1.0f);

        giveStat(player, 10);

        assertTrue(stat.takeStatFrom(player, 5));
        assertEquals(5, getHas(player));

        postTest(player);
    }

    @Test
    void testNotEnoughDoesNotTakeStat() {
        Stat stat = getStat(1.0f);

        giveStat(player, 5);

        assertFalse(stat.takeStatFrom(player, 10));
        assertEquals(5, getHas(player));

        postTest(player);
    }

    @Test
    void testTakeSmallRatioStat() {
        Stat stat = getStat(0.5f);

        giveStat(player, 5);

        assertTrue(stat.takeStatFrom(player, 10));
        assertEquals(0, getHas(player));

        postTest(player);
    }

    @Test
    void testTakeBigRatioStat() {
        Stat stat = getStat(2.0f);

        giveStat(player, 6);

        assertTrue(stat.takeStatFrom(player, 3));
        assertEquals(0, getHas(player));

        postTest(player);
    }

    @Test
    void testNotTakeRatioWhenRawWouldBeFineStat() {
        Stat stat = getStat(2.0f);

        giveStat(player, 9);

        assertFalse(stat.takeStatFrom(player, 5));
        assertEquals(9, getHas(player));

        postTest(player);
    }

    @Test
    void testTakeMinimumRatioStat() {
        Stat stat = getStat(0.0001f);

        giveStat(player, 5);

        assertTrue(stat.takeStatFrom(player, 1));
        assertEquals(4, getHas(player));

        postTest(player);
    }
}
