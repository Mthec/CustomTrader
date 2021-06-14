package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FavorTests extends StatTests {
    @Override
    protected Stat getStat(float ratio) {
        return create(Favor.class.getSimpleName(), ratio);
    }

    @Override
    protected void giveStat(Player player, int amount) {
        try {
            player.setFavor(amount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testTakeBigRatioStat() {
        Stat stat = getStat(2.0f);

        giveStat(player, 6);
        assert stat.creatureHas(player) == 12;

        assertTrue(stat.takeStatFrom(player, 3));
        assertEquals(9, stat.creatureHas(player));

        postTest(player);
    }

    @Test
    void testTakeMinimumRatioStat() {
        Stat stat = getStat(1000f);

        giveStat(player, 1);

        assertTrue(stat.takeStatFrom(player, 1));
        assertEquals(999, stat.creatureHas(player));

        postTest(player);
    }
}
