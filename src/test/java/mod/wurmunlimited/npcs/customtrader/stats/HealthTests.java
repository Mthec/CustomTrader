package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class HealthTests extends StatTests {
    @Override
    protected Stat getStat(float ratio) {
        return create(Health.class.getSimpleName(), ratio);
    }

    @Override
    protected void giveStat(Player player, int amount) {
        player.getStatus().damage = 65534 - amount;
    }

    @Override
    protected int getHas(Player player) {
        return 65534 - player.getStatus().damage;
    }

    @Override
    protected void postTest(Player player) {
        assertFalse(player.isDead());
        assertFalse(player.getStatus().damage >= 65535);
    }
}
