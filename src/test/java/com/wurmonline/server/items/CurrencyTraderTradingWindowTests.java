package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CurrencyTraderTradeHandler;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyTraderTradingWindowTests extends CustomTraderTest {
    private Creature trader;
    private Player player;
    private int currency;
    private CurrencyTraderTrade trade;
    private Item item;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        player = factory.createNewPlayer();
        currency = ItemList.medallionHota;
        trader = factory.createNewCurrencyTrader(currency);
        assert trader.getShop() != null;
        CustomTraderDatabase.addStockItemTo(trader, 5, 5, 5, (byte)0, (byte)0, 5, new Enchantment[0], (byte)0, "", 5, 5, 0);
        CustomTraderDatabase.restock(trader);
        trade = new CurrencyTraderTrade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        CurrencyTraderTradeHandler handler = new CurrencyTraderTradeHandler(trader, trade);
        ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        handler.addItemsToTrade();
        item = factory.createNewItem(currency);
        player.getInventory().insertItem(item);
    }

    @Test
    void testCurrencyDestroyedWhenAccepted() {
        TradingWindow window = trade.getTradingWindow(4);
        window.addItem(item);

        window.swapOwners();

        assertThrows(NoSuchItemException.class, () -> Items.getItem(item.getWurmId()));
        assertEquals(1, factory.getShop(trader).getMoneyEarnedLife());
    }

    @Test
    void testMultipleCurrencyCountedCorrectly() {
        for (int i = 0; i < 4; i++) {
            Item item = factory.createNewItem(currency);
            player.getInventory().insertItem(item);
        }

        TradingWindow window = trade.getTradingWindow(4);
        player.getInventory().getItems().forEach(window::addItem);

        window.swapOwners();

        assertEquals(5, factory.getShop(trader).getMoneyEarnedLife());
    }

    @Test
    void testPlayerGetsItem() {
        TradingWindow window = trade.getTradingWindow(3);
        Item prize = trade.getTradingWindow(1).getItems()[0];
        trade.getTradingWindow(1).removeItem(prize);
        window.addItem(prize);

        window.swapOwners();

        assertTrue(player.getInventory().getItems().contains(prize));
        assertFalse(trader.getInventory().getItems().contains(prize));
    }
}
