package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomTraderTradeHandlerTests extends CustomTraderTest {
    private Creature trader;
    private Player player;
    private Trade trade;
    private CustomTraderTradeHandler handler;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        player = factory.createNewPlayer();
        trader = factory.createNewCustomTrader();
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 5, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new CustomTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
    }

    @Test
    void testAllItemsAddedToTradeWindow() {
        TradeHandler tradeHandler = new CustomTraderTradeHandler(trader, trade);
        tradeHandler.addItemsToTrade();

        assert trader.getInventory().getItems().size() == 5;
        assertEquals(5, trade.getTradingWindow(1).getAllItems().length);
    }

    @Test
    void testGetTraderSellPriceForItem() {
        TradeHandler tradeHandler = new CustomTraderTradeHandler(trader, trade);
        tradeHandler.addItemsToTrade();

        Item item1 = factory.createNewItem();
        item1.setPrice(100);
        assertEquals(100, tradeHandler.getTraderSellPriceForItem(item1, trade.getTradingWindow(1)));
        Item item2 = factory.createNewItem();
        item2.setPrice(167845345);
        assertEquals(167845345, tradeHandler.getTraderSellPriceForItem(item2, trade.getTradingWindow(1)));
    }

    @Test
    void testGetTraderBuyPriceForItem() {
        TradeHandler tradeHandler = new CustomTraderTradeHandler(trader, trade);
        tradeHandler.addItemsToTrade();

        Item item1 = factory.createNewItem();
        item1.setPrice(100);
        assertEquals(0, tradeHandler.getTraderBuyPriceForItem(item1));
        Item item2 = factory.createNewItem();
        item2.setPrice(167845345);
        assertEquals(0, tradeHandler.getTraderBuyPriceForItem(item2));
    }

    // Balance

    @Test
    void testBalancesOnce() {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        assertEquals(1, factory.getCommunicator(player).getMessages().length);
        handler.balance();
        assertEquals(2, factory.getCommunicator(player).getMessages().length);
        handler.balance();
        assertEquals(2, factory.getCommunicator(player).getMessages().length);
    }

    @Test
    void testBalancesTwiceIfTradeChanged() {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        assertEquals(1, factory.getCommunicator(player).getMessages().length);
        handler.balance();
        assertEquals(2, factory.getCommunicator(player).getMessages().length);
        handler.tradeChanged();
        handler.balance();
        assertEquals(3, factory.getCommunicator(player).getMessages().length);
    }

    @Test
    void testOnlyCoinsSucked() {
        Item item = factory.createNewItem();
        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        handler.balance();
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertTrue(trade.getTradingWindow(4).getAllItems()[0].isCoin());
    }

    @Test
    void testPositiveDiffSendsMessage() throws NoSuchFieldException, IllegalAccessException {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 50 iron coins"));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, Trade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testNegativeDiffAddsMoney() throws NoSuchFieldException, IllegalAccessException {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(coin);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertThat(Arrays.asList(trade.getTradingWindow(3).getAllItems()), containsCoinsOfValue(50));
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, Trade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void test0DiffDoesNotSendMessageOrAddMoney() throws NoSuchFieldException, IllegalAccessException {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        Arrays.asList(Economy.getEconomy().getCoinsFor(50)).forEach(c -> trade.getTradingWindow(2).addItem(c));

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(3).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, Trade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testConsecutiveBalancesDoNotAddMoreMoney() {
        Item item = factory.createNewItem();
        item.setPrice(50);
        trade.getTradingWindow(3).addItem(item);

        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(coin);

        assertThat(Arrays.asList(trade.getTradingWindow(3).getAllItems()), containsCoinsOfValue(0));
        handler.balance();
        assertThat(Arrays.asList(trade.getTradingWindow(3).getAllItems()), containsCoinsOfValue(50));
        handler.tradeChanged();
        handler.balance();
        assertThat(Arrays.asList(trade.getTradingWindow(3).getAllItems()), containsCoinsOfValue(50));
    }

    @Test
    void testTradeDoesNotRestock() throws CustomTraderDatabase.StockUpdateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 1, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        assert trader.getInventory().getItems().size() == 1;

        handler.addItemsToTrade();
        assert trade.getTradingWindow(1).getAllItems().length == 1;
        Item item = trade.getTradingWindow(1).getAllItems()[0];
        trade.getTradingWindow(1).removeItem(item);
        trade.getTradingWindow(3).addItem(item);

        player.getInventory().insertItem(factory.createNewIronCoin());
        trade.getTradingWindow(2).addItem(player.getInventory().getFirstContainedItem());
        handler.balance();

        assert trade.getTradingWindow(1).getAllItems().length == 0;
        assert trade.getTradingWindow(2).getAllItems().length == 0;
        assert trade.getTradingWindow(3).getAllItems().length == 1;
        assert trade.getTradingWindow(4).getAllItems().length == 1;

        Method methodMock = mock(Method.class);
        when(methodMock.invoke(any(), any())).thenAnswer((Answer<Boolean>)invocation -> {
            trade.setSatisfied(player, true, trade.getCurrentCounter());
            return true;
        });
        ReflectionUtil.callPrivateMethod(new CustomTraderMod(), CustomTraderMod.class.getDeclaredMethod("makeTrade", Object.class, Method.class, Object[].class),
                trade, methodMock, new Object[0]);

        assertThat(player, receivedMessageContaining("completed"));
        assertEquals(0, trader.getInventory().getItems().size());
    }
}
