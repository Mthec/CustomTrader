package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CurrencyTraderManagementQuestion;
import com.wurmonline.server.questions.CustomTraderManagementQuestion;
import com.wurmonline.server.questions.StatTraderManagementQuestion;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ManageCustomTraderActionTests extends CustomTraderTest {
    private Player gm;
    private Player nonGm;
    private Creature customTrader;
    private Creature currencyTrader;
    private Creature statTrader;
    private Item wand;
    private ManageCustomTraderAction action;
    private final Action act = mock(Action.class);

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        ActionEntryBuilder.init();
        action = new ManageCustomTraderAction();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        customTrader = factory.createNewCustomTrader();
        currencyTrader = factory.createNewCurrencyTrader();
        statTrader = factory.createNewStatTrader();
        nonGm = factory.createNewPlayer();
        wand = factory.createNewItem(ItemList.wandGM);
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourForCustomTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(gm, wand, customTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Manage", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForCurrencyTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(gm, wand, currencyTrader);
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("Manage", entries.get(0).getActionString());
    }

    @Test
    void testGetBehaviourForStatTrader() {
        List<ActionEntry> entries = action.getBehavioursFor(gm, wand, statTrader);
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
        assertTrue(action.action(act, gm, wand, customTrader, action.getActionId(), 0f));
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new CustomTraderManagementQuestion(gm, customTrader).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testActionCurrencyTrader() {
        assertTrue(action.action(act, gm, wand, currencyTrader, action.getActionId(), 0f));
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new CurrencyTraderManagementQuestion(gm, currencyTrader).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testActionStatTrader() {
        assertTrue(action.action(act, gm, wand, statTrader, action.getActionId(), 0f));
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new StatTraderManagementQuestion(gm, statTrader).sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    void testActionNotGM() {
        assertTrue(action.action(act, nonGm, wand, customTrader, action.getActionId(), 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionNotWand() {
        Item notWand = factory.createNewItem();
        assert notWand.getTemplateId() != ItemList.wandGM;
        assertTrue(action.action(act, gm, notWand, customTrader, action.getActionId(), 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionNotCustomTrader() {
        assertTrue(action.action(act, gm, wand, factory.createNewTrader(), action.getActionId(), 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testActionIncorrectActionId() {
        assertTrue(action.action(act, gm, wand, customTrader, (short)(action.getActionId() + 1), 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }
}
