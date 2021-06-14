package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CurrencyTraderTradeHandler;
import com.wurmonline.server.creatures.StatTraderTradeHandler;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.items.CurrencyTraderTrade;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.StatTraderTrade;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.FavorPriest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class OtherTraderTradeActionTests extends CustomTraderTest {
    private Player player;
    private Creature currencyTrader;
    private Creature statTrader;
    private OtherTraderTradeAction action;
    private final Action act = mock(Action.class);

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        ActionEntryBuilder.init();
        action = new OtherTraderTradeAction();
        currencyTrader = factory.createNewCurrencyTrader();
        statTrader = factory.createNewStatTrader();
        player = factory.createNewPlayer();
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourForCurrencyTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(player, currencyTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Trade", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForStatTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(player, statTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Trade", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForItemCurrencyTrader() {
        Item item = factory.createNewItem();
        List<ActionEntry> entries = action.getBehavioursFor(player, item, currencyTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Trade", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForItemStatTrader() {
        Item item = factory.createNewItem();
        List<ActionEntry> entries = action.getBehavioursFor(player, item, statTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Trade", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForNotCustomTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(player, factory.createNewTrader());
        assertNull(entries);
    }

    // action

    private void createHandlerFor(Creature trader) {
        try {
            TradeHandler handler;
            if (trader == currencyTrader) {
                handler = new CurrencyTraderTradeHandler(trader, new CurrencyTraderTrade(player, trader));
            } else {
                handler = new StatTraderTradeHandler(trader, new StatTraderTrade(player, trader, Objects.requireNonNull(CustomTraderDatabase.getStatFor(trader))));
            }
            ReflectionUtil.setPrivateField(trader, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testActionCurrencyTrader() {
        createHandlerFor(currencyTrader);
        assertTrue(action.action(act, player, currencyTrader, action.getActionId(), 0f));
        assertTrue(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionStatTrader() {
        createHandlerFor(statTrader);
        assertTrue(action.action(act, player, statTrader, action.getActionId(), 0f));
        assertTrue(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionWithItemCurrencyTrader() {
        Item item = factory.createNewItem();
        createHandlerFor(currencyTrader);
        assertTrue(action.action(act, player, item, currencyTrader, action.getActionId(), 0f));
        assertTrue(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionWithItemStatTrader() {
        Item item = factory.createNewItem();
        createHandlerFor(statTrader);
        assertTrue(action.action(act, player, item, statTrader, action.getActionId(), 0f));
        assertTrue(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionNotCustomTrader() {
        assertTrue(action.action(act, player, factory.createNewTrader(), action.getActionId(), 0f));
        assertFalse(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionIncorrectActionId() {
        assertTrue(action.action(act, player, currencyTrader, (short)(action.getActionId() + 1), 0f));
        assertFalse(factory.getCommunicator(player).sentStartTrading);
    }

    @Test
    void testActionStatTraderFavorPriestNotPriest() {
        assert !player.isPriest();
        CustomTraderDatabase.setStatFor(statTrader, create(FavorPriest.class.getSimpleName(), 1.0f));
        createHandlerFor(statTrader);
        assertTrue(action.action(act, player, statTrader, action.getActionId(), 0f));
        assertFalse(factory.getCommunicator(player).sentStartTrading);
    }
}
