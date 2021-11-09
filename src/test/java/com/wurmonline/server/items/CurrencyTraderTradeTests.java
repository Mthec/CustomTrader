package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CurrencyTraderTradeHandler;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CurrencyTraderTradeTests extends CustomTraderTest {
    private Creature trader;
    private Player player;
    private CurrencyTraderTrade trade;
    private CurrencyTraderTradeHandler handler;
    private Item item;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        player = factory.createNewPlayer();
        int currency = ItemList.medallionHota;
        trader = factory.createNewCurrencyTrader(currency);
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 1, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, "", 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new CurrencyTraderTrade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new CurrencyTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
        item = factory.createNewItem(currency);
        player.getInventory().insertItem(item);
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
        assertTrue(trade.getTradingWindow(2).mayAddFromInventory(player, item));
        trade.getTradingWindow(2).addItem(item);

        handler.balance();
        setSatisfied(player);

        assertFalse(player.getInventory().getItems().contains(item));
        assertFalse(trader.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testCorrectTradingWindowsCreated() {
        assertAll(
                () -> assertTrue(trade.getTradingWindow(1) instanceof CurrencyTradingWindow),
                () -> assertTrue(trade.getTradingWindow(2) instanceof CurrencyTradingWindow),
                () -> assertTrue(trade.getTradingWindow(3) instanceof CurrencyTradingWindow),
                () -> assertTrue(trade.getTradingWindow(4) instanceof CurrencyTradingWindow)
        );
    }

    @Test
    void testTradeDoesNotCompleteIfPlayerDoesNotAccept() {
        assertTrue(trade.getTradingWindow(2).mayAddFromInventory(player, item));
        trade.getTradingWindow(2).addItem(item);

        selectPrize();
        handler.balance();

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(trader.getInventory().getItems().contains(item));
        assertThat(player, didNotReceiveMessageContaining("completed successfully"));
    }

    @Test
    void testTradeDoesNotCompleteIfCurrencyTraderDoesNotAccept() {
        selectPrize();
        handler.balance();

        assertTrue(trade.getTradingWindow(2).mayAddFromInventory(player, item));
        trade.getTradingWindow(2).addItem(item);

        handler.balance();
        trade.setSatisfied(trader, false, trade.getCurrentCounter());
        setSatisfied(player);

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(trader.getInventory().getItems().contains(item));
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
}
