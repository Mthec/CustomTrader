package mod.wurmunlimited.npcs.customtrader.stats;

import com.wurmonline.server.players.Player;

public class KarmaTests extends StatTests {
    @Override
    protected Stat getStat(float ratio) {
        return Karma.create(Karma.class.getSimpleName(), ratio);
    }

    @Override
    protected void giveStat(Player player, int amount) {
        player.setKarma(amount);
    }

    @Override
    protected int getHas(Player player) {
        return player.getKarma();
    }
}
