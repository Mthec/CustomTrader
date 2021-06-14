package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.OtherTraderTrade;
import com.wurmonline.server.items.StatTraderTrade;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Favor;
import mod.wurmunlimited.npcs.customtrader.stats.FavorPriest;
import mod.wurmunlimited.npcs.customtrader.stats.Karma;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatTraderTradeHandlerTests extends CustomTraderTest {
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
        assert stat != null;
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

    @Test
    void testMessageInCoin() {
        factory.getCommunicator(player).clear();
        player.setKarma(1234567);
        handler = new StatTraderTradeHandler(trader, trade);
        assertThat(player, receivedMessageContaining("1234567"));
        assertThat(player, receivedMessageContaining("1g, 23s, 45c, 67i"));
    }

    @Test
    void testMessageIfZero() {
        factory.getCommunicator(player).clear();
        player.setKarma(0);
        handler = new StatTraderTradeHandler(trader, trade);
        assertThat(player, receivedMessageContaining("you have 0"));
        assertThat(player, didNotReceiveMessageContaining("equivalent"));
    }

    @Test
    void testAllItemsAddedToTradeWindow() {
        assert trader.getInventory().getItems().size() == 5;
        assertEquals(5, trade.getTradingWindow(1).getAllItems().length);
    }

    @Test
    void testGetPriceFor() {
        TradingWindow window = trade.getTradingWindow(1);
        Item item = window.getItems()[0];

        assertEquals(1, handler.getTraderSellPriceForItem(item, window));
    }

    // Balance

    @Test
    void testBalancesOnce() throws NoSuchFieldException, IllegalAccessException {
        Item selectedItem = trade.getTradingWindow(1).getAllItems()[0];
        player.setKarma(0);
        selectPrize();

        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, OtherTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
        player.setKarma(1);
        handler.balance();
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, OtherTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testNothingSucked() {
        selectPrize();
        Item item = factory.createNewItem();
        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);
        handler.balance();

        assertEquals(2, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(0, trade.getTradingWindow(4).getAllItems().length);
    }

    @Test
    void testPrice() throws NoSuchFieldException, IllegalAccessException, CustomTraderDatabase.StockUpdateException {
        trade.end(trader, true);
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 10, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        trade = new StatTraderTrade(player, trader, stat);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new StatTraderTradeHandler(trader, trade);
        handler.addItemsToTrade();
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        player.setKarma(10);
        selectPrize();
        handler.balance();

        assertThat(player, didNotReceiveMessageContaining("demands"));
    }

    @Test
    void testNotEnoughToMeetPrice() throws NoSuchFieldException, IllegalAccessException, CustomTraderDatabase.StockUpdateException {
        trade.end(trader, true);
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 10, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        trade = new StatTraderTrade(player, trader, stat);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new StatTraderTradeHandler(trader, trade);
        handler.addItemsToTrade();
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);

        player.setKarma(1);
        selectPrize();
        handler.balance();

        assertThat(player, receivedMessageContaining("demands 9 more Karma"));
    }

    @Test
    void testPositiveDiffSendsMessage() throws NoSuchFieldException, IllegalAccessException {
        player.setKarma(1);
        selectPrize();
        selectPrize();
        handler.balance();

        assertThat(player, receivedMessageContaining("demands 1 more Karma"));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, OtherTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testTradeDoesNotRestock() throws CustomTraderDatabase.StockUpdateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (Item item : trade.getTradingWindow(1).getAllItems()) {
            trade.getTradingWindow(1).removeItem(item);
        }
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        assert trader.getInventory().getItems().size() == 1;

        handler = new StatTraderTradeHandler(trader, trade);
        handler.addItemsToTrade();
        player.setKarma(1);
        assert trade.getTradingWindow(1).getAllItems().length == 1;
        selectPrize();
        handler.balance();

        assert trade.getTradingWindow(1).getAllItems().length == 0;
        assert trade.getTradingWindow(2).getAllItems().length == 0;
        assert trade.getTradingWindow(3).getAllItems().length == 1;
        assert trade.getTradingWindow(4).getAllItems().length == 0;

        Method methodMock = mock(Method.class);
        when(methodMock.invoke(any(), any())).thenAnswer((Answer<Boolean>)invocation -> {
            trade.setSatisfied(player, true, trade.getCurrentCounter());
            return true;
        });
        ReflectionUtil.callPrivateMethod(new CustomTraderMod(), CustomTraderMod.class.getDeclaredMethod("makeTrade", Object.class, Method.class, Object[].class),
                trade, methodMock, new Object[0]);

        assertThat(player, receivedMessageContaining("completed"));
        assertEquals(1, player.getInventory().getItemCount());
        assertEquals(0, trader.getInventory().getItems().size());
    }

    @Test
    void testNonPriestsBlockedIfFavorPriest() throws NoSuchFieldException, IllegalAccessException, IOException {
        assert !player.isPriest();
        player.setFavor(2);
        CustomTraderDatabase.setStatFor(trader, create(FavorPriest.class.getSimpleName(), 1.0f));

        handler = new StatTraderTradeHandler(trader, trade);
        assertThat(player, receivedMessageContaining("only offer my services to priests"));
        assertTrue(handler.aborted);
    }

    @Test
    void testPriestsNotBlockedIfFavorPriest() throws NoSuchFieldException, IllegalAccessException, IOException {
        player.setPriest(true);
        player.setFavor(2);
        CustomTraderDatabase.setStatFor(trader, create(FavorPriest.class.getSimpleName(), 1.0f));

        handler = new StatTraderTradeHandler(trader, trade);
        assertThat(player, receivedMessageContaining("I will trade"));
        assertFalse(handler.aborted);
    }

    @Test
    void testNonPriestsNotBlockedIfFavor() throws NoSuchFieldException, IllegalAccessException, IOException {
        assert !player.isPriest();
        player.setFavor(2);
        CustomTraderDatabase.setStatFor(trader, create(Favor.class.getSimpleName(), 1.0f));

        handler = new StatTraderTradeHandler(trader, trade);
        assertThat(player, didNotReceiveMessageContaining("only offer my services to priests"));
        assertThat(player, receivedMessageContaining("I will trade"));
        assertFalse(handler.aborted);
    }
}