package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CurrencyTraderManagementQuestion;
import com.wurmonline.server.questions.CustomTraderManagementQuestion;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ManageCustomTraderActionTests extends CustomTraderTest {
    private Player gm;
    private Player nonGm;
    private Creature customTrader;
    private Creature currencyTrader;
    private Item wand;
    private ManageCustomTraderAction action;
    private Action act = mock(Action.class);

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        ActionEntryBuilder.init();
        action = new ManageCustomTraderAction();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        customTrader = factory.createNewCustomTrader();
        currencyTrader = factory.createNewCurrencyTrader();
        nonGm = factory.createNewPlayer();
        wand = factory.createNewItem(ItemList.wandGM);
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourFor() {
        List<ActionEntry> entries = action.getBehavioursFor(gm, wand, customTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Manage", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForNotWand() {
        Item notWand = factory.createNewItem();
        assert notWand.getTemplateId() != ItemList.wandGM;
        List<ActionEntry> entries = action.getBehavioursFor(gm, notWand, customTrader);
        assertNull(entries);
    }

    @Test
    void testGetBehaviourForNotGM() {
        List<ActionEntry> entries = action.getBehavioursFor(nonGm, wand, customTrader);
        assertNull(entries);
    }

    @Test
    void testGetBehaviourForNotCustomTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(gm, wand, factory.createNewTrader());
        assertNull(entries);
    }

    // action

    @Test
    void testActionCustomTrader() {
        boolean result = action.action(act, gm, wand, customTrader, action.getActionId(), 0f);
        assertTrue(result);
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new CustomTraderManagementQuestion(gm, customTrader).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testActionCurrencyTrader() {
        boolean result = action.action(act, gm, wand, currencyTrader, action.getActionId(), 0f);
        assertTrue(result);
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new CurrencyTraderManagementQuestion(gm, currencyTrader).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testActionNotGM() {
        boolean result = action.action(act, nonGm, wand, customTrader, action.getActionId(), 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionNotWand() {
        Item notWand = factory.createNewItem();
        assert notWand.getTemplateId() != ItemList.wandGM;
        boolean result = action.action(act, gm, notWand, customTrader, action.getActionId(), 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionNotCustomTrader() {
        boolean result = action.action(act, gm, wand, factory.createNewTrader(), action.getActionId(), 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionIncorrectActionId() {
        boolean result = action.action(act, gm, wand, customTrader, (short)(action.getActionId() + 1), 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }
}
