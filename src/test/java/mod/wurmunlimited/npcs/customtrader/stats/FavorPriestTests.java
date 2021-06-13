package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FavorPriestTests extends StatTests {
    @Override
    protected Stat getStat(float ratio) {
        return create(FavorPriest.class.getSimpleName(), ratio);
    }

    @Override
    protected void giveStat(Player player, int amount) {
        try {
            player.setPriest(true);
            player.setFavor(amount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getHas(Player player) {
        if (!player.isPriest()) {
            throw new RuntimeException("Player was not a priest.");
        }
        return (int)player.getFavor();
    }

    @Override
    @Test
    void testTakeMinimumRatioStat() {
        Player player = CustomTraderObjectsFactory.getCurrent().createNewPlayer();
        Stat stat = getStat(0.0001f);

        try {
            player.setPriest(true);
            player.setFavor(0.0002f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertTrue(stat.takeStatFrom(player, 1));
        assertEquals(0.0001f, player.getFavor());
    }

    @Test
    void testIsNotPriest() {
        Player player = CustomTraderObjectsFactory.getCurrent().createNewPlayer();
        assert !player.isPriest();
        Stat stat = getStat(1.0f);

        try {
            player.setFavor(10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertFalse(stat.takeStatFrom(player, 1));
        assertEquals(10, player.getFavor());
    }
}
