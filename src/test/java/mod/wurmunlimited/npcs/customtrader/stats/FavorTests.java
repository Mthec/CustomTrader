package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderObjectsFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FavorTests extends StatTests {
    @Override
    protected Stat getStat(float ratio) {
        return Favor.create(Favor.class.getSimpleName(), ratio);
    }

    @Override
    protected void giveStat(Player player, int amount) {
        try {
            player.setFavor(amount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getHas(Player player) {
        return (int)player.getFavor();
    }

    @Override
    @Test
    void testTakeMinimumRatioStat() {
        Player player = CustomTraderObjectsFactory.getCurrent().createNewPlayer();
        Stat stat = getStat(0.0001f);

        try {
            player.setFavor(0.0002f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertTrue(stat.takeStatFrom(player, 1));
        assertEquals(0.0001f, player.getFavor());
    }
}
