package com.wurmonline.server.creatures;

import com.wurmonline.server.items.CurrencyTraderTrade;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
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

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CurrencyTraderTradeHandlerTests extends CustomTraderTest {
    private Creature trader;
    private Player player;
    private int currency;
    private CurrencyTraderTrade trade;
    private CurrencyTraderTradeHandler handler;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        player = factory.createNewPlayer();
        currency = ItemList.medallionHota;
        trader = factory.createNewCurrencyTrader(currency, 1);
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 1, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new CurrencyTraderTrade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new CurrencyTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
    }

    private void selectPrize() {
        Item prize = trade.getTradingWindow(1).getItems()[0];
        trade.getTradingWindow(1).removeItem(prize);
        trade.getTradingWindow(3).addItem(prize);
    }

    @Test
    void testAllItemsAddedToTradeWindow() {
        assert trader.getInventory().getItems().size() == 5;
        assertEquals(5, trade.getTradingWindow(1).getAllItems().length);
    }

    // Balance

    @Test
    void testBalancesOnce() throws NoSuchFieldException, IllegalAccessException {
        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item);

        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, CurrencyTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
        handler.balance();
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, CurrencyTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testOnlyCurrencySucked() {
        selectPrize();
        Item item = factory.createNewItem(currency);
        Item coin = factory.createNewCopperCoin();
        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        handler.balance();
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertEquals(currency, trade.getTradingWindow(4).getAllItems()[0].getTemplateId());
    }

    @Test
    void testPrice() throws NoSuchFieldException, IllegalAccessException, CustomTraderDatabase.StockUpdateException {
        trade.end(trader, true);
        CustomTraderDatabase.setCurrencyFor(trader, currency);
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 10, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        trade = new CurrencyTraderTrade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new CurrencyTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
        selectPrize();
        for (int i = 0; i < 10; i++) {
            Item item = factory.createNewItem(currency);
            trade.getTradingWindow(4).addItem(item);
        }

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(10, trade.getTradingWindow(4).getAllItems().length);
    }

    @Test
    void testNotEnoughToMeetPrice() throws NoSuchFieldException, IllegalAccessException, CustomTraderDatabase.StockUpdateException {
        trade.end(trader, true);
        CustomTraderDatabase.setCurrencyFor(trader, currency);
        CustomTraderDatabase.deleteAllStockFor(trader);
        CustomTraderDatabase.addStockItemTo(trader, 1, 1, 10, (byte)0, (byte)2, 1, new Enchantment[0], (byte)0, 1, 0, 100);
        CustomTraderDatabase.fullyStock(trader);
        trade = new CurrencyTraderTrade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        handler = new CurrencyTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();

        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 9 more medallions"));
    }

    @Test
    void testPositiveDiffSendsMessage() throws NoSuchFieldException, IllegalAccessException {
        selectPrize();
        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(2).addItem(item);

        handler.balance();
        assertThat(player, receivedMessageContaining("demands 1 more medallion"));
        assertFalse((boolean)ReflectionUtil.getPrivateField(trade, CurrencyTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testNegativeDiffRemovesCurrency() throws NoSuchFieldException, IllegalAccessException {
        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item);
        Item item2 = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item2);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(2).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, CurrencyTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void test0DiffDoesNotSendMessageOrAddMoney() throws NoSuchFieldException, IllegalAccessException {
        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item);

        handler.balance();
        assertThat(player, didNotReceiveMessageContaining("demands"));
        assertEquals(1, trade.getTradingWindow(3).getAllItems().length);
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        assertTrue((boolean)ReflectionUtil.getPrivateField(trade, CurrencyTraderTrade.class.getDeclaredField("creatureTwoSatisfied")));
    }

    @Test
    void testConsecutiveBalancesDoNotAlterSelectedCurrency() {
        selectPrize();
        Item item = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item);
        Item item2 = factory.createNewItem(currency);
        trade.getTradingWindow(4).addItem(item2);

        assertEquals(2, trade.getTradingWindow(4).getAllItems().length);
        handler.balance();
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
        handler.tradeChanged();
        handler.balance();
        assertEquals(1, trade.getTradingWindow(4).getAllItems().length);
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

        handler = new CurrencyTraderTradeHandler(trader, trade);
        handler.addItemsToTrade();
        assert trade.getTradingWindow(1).getAllItems().length == 1;
        selectPrize();

        Item item = factory.createNewItem(currency);
        player.getInventory().insertItem(item);
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
