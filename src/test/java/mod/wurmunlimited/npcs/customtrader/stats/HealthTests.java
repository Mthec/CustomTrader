package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.NoSuchSkillException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    protected void postTest(Player player) {
        assertFalse(player.isDead());
        assertFalse(player.getStatus().damage >= 65535);
        assertFalse(player.getStatus().damage < 0);
    }

    @Test
    void testLargeValue() {
        player.getStatus().damage = 5050;
        Stat stat = getStat(100.0f);

        assertEquals(6048400, stat.creatureHas(player));
    }

    @Test
    void testPlayerStrengthDoesNotAffectDamage() throws NoSuchSkillException {
        assert player.getStrengthSkill() == 1;
        assert player.getStatus().damage == 0;
        int halfDamage = 65534 / 2;
        Stat stat = getStat(1.0f);
        stat.takeStatFrom(player, halfDamage);

        assertAlmostEquals(halfDamage, stat.creatureHas(player));

        player.getBody().healFully();
        player.getSkills().getSkill(102).setKnowledge(99, false);
        assert player.getStrengthSkill() == 99;
        stat.takeStatFrom(player, halfDamage);

        assertAlmostEquals(halfDamage, stat.creatureHas(player));
    }

    @Test
    void testTakeStatMax() {
        Stat stat = getStat(1.0f);

        assertTrue(stat.takeStatFrom(player, 65534));
        assertAlmostEquals(65534, player.getStatus().damage);
        assertAlmostEquals(1, stat.creatureHas(player));

        postTest(player);
    }
}
