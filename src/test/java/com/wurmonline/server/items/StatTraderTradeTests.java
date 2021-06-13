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

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class StatTraderTradeTests extends CustomTraderTest {
    private Creature trader;
    private Stat stat;
    private Player player;
    private StatTraderTrade trade;
    private StatTraderTradeHandler handler;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        player = factory.createNewPlayer();
        stat = create(Karma.class.getSimpleName(), 1.0f);
        trader = factory.createNewStatTrader(stat);
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 1, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new StatTraderTrade(player, trader, stat);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new StatTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
    }

    private void selectPrize() {
        Item prize = trade.getTradingWindow(1).getItems()[0];
        trade.getTradingWindow(1).removeItem(prize);
        trade.getTradingWindow(3).addItem(prize);
    }

    private void setSatisfied(Creature creature) {
        trade.setSatisfied(creature, true, trade.getCurrentCounter());
    }

    @Test
    void testTradeCompletion() {
        selectPrize();
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        assertFalse(trade.getTradingWindow(1).mayAddFromInventory(player, item));
        assertFalse(trade.getTradingWindow(3).mayAddFromInventory(player, item));
        assertFalse(trade.getTradingWindow(4).mayAddFromInventory(player, item));

        player.setKarma(1);
        handler.balance();
        setSatisfied(player);

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(trader.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testCorrectTradingWindowsCreated() {
        assertAll(
                () -> assertTrue(trade.getTradingWindow(1) instanceof StatTradingWindow),
                () -> assertTrue(trade.getTradingWindow(2) instanceof StatTradingWindow),
                () -> assertTrue(trade.getTradingWindow(3) instanceof StatTradingWindow),
                () -> assertTrue(trade.getTradingWindow(4) instanceof StatTradingWindow)
        );
    }

    @Test
    void testTradeDoesNotCompleteIfPlayerDoesNotAccept() {
        selectPrize();
        player.setKarma(1);
        handler.balance();

        assertEquals(0, player.getInventory().getItemCount());
        assertEquals(5, trader.getInventory().getItemCount());
        assertThat(player, didNotReceiveMessageContaining("completed successfully"));
    }

    @Test
    void testTradeDoesNotCompleteIfStatTraderDoesNotAccept() {
        selectPrize();
        assert player.getKarma() == 0;

        handler.balance();
        trade.setSatisfied(trader, false, trade.getCurrentCounter());
        setSatisfied(player);

        assertThat(player, didNotReceiveMessageContaining("I will need"));
        assertThat(player, didNotReceiveMessageContaining("completed successfully"));
    }

    @Test
    void testTradeWindowClosesOnTradeEnd() {
        setSatisfied(trader);
        setSatisfied(player);

        assertNull(trader.getTrade());
        assertNull(player.getTrade());
        assertThat(player, didNotReceiveMessageContaining("withdraw from the trade"));
    }

    @Test
    void testStatTradingWindowReceivesStat() throws NoSuchFieldException, IllegalAccessException {
        assertEquals(stat, ReflectionUtil.getPrivateField(trade.getTradingWindow(1), StatTradingWindow.class.getDeclaredField("stat")));
    }
}
