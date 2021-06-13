package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.StatTraderTradeHandler;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Karma;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class StatTraderTradingWindowTests extends CustomTraderTest {
    private Creature trader;
    private Player player;
    private int currency;
    private StatTraderTrade trade;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        player = factory.createNewPlayer();
        Stat stat = Karma.create(Karma.class.getSimpleName(), 1.0f);
        assert stat != null;
        trader = factory.createNewStatTrader(stat);
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 5, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new StatTraderTrade(player, trader, stat);
        player.setTrade(trade);
        trader.setTrade(trade);
        StatTraderTradeHandler handler = new StatTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
    }

    @Test
    void testItemNotTransferredWhenAccepted() {
        Item item = factory.createNewItem();
        TradingWindow window = trade.getTradingWindow(4);
        window.addItem(item);

        window.swapOwners();

        assertEquals(5, trader.getInventory().getItemCount());
    }

    @Test
    void testPlayerChargedCorrectly() {
        player.setKarma(25);
        TradingWindow sourceWindow = trade.getTradingWindow(1);
        TradingWindow window = trade.getTradingWindow(3);
        Arrays.asList(sourceWindow.getAllItems()).forEach(i -> {
            sourceWindow.removeItem(i);
            window.addItem(i);
        });
        assert player.getInventory().getItemCount() == 0;
        assert trader.getInventory().getItemCount() == 5;

        window.swapOwners();

        assertEquals(25, factory.getShop(trader).getMoneyEarnedLife());
        assertEquals(0, player.getKarma());
        assertEquals(5, player.getInventory().getItemCount());
        assertEquals(0, trader.getInventory().getItemCount());
    }

    @Test
    void testPlayerGetsItem() {
        player.setKarma(5);
        TradingWindow window = trade.getTradingWindow(3);
        Item prize = trade.getTradingWindow(1).getItems()[0];
        trade.getTradingWindow(1).removeItem(prize);
        window.addItem(prize);

        window.swapOwners();

        assertTrue(player.getInventory().getItems().contains(prize));
        assertFalse(trader.getInventory().getItems().contains(prize));
    }

    @Test
    void testValidateTrade() {
        player.setKarma(24);

        TradingWindow windowOffer = trade.getTradingWindow(1);
        TradingWindow windowRequest = trade.getTradingWindow(3);

        int counter = 0;
        for (Item item : windowOffer.getAllItems()) {
            windowOffer.removeItem(item);
            windowRequest.addItem(item);

            if (++counter == 5)
                break;

            assertTrue(windowRequest.validateTrade());
        }

        assertFalse(windowRequest.validateTrade());
    }
}
