package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.PlaceCurrencyTraderQuestion;
import com.wurmonline.server.questions.PlaceCustomTraderQuestion;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.Assert;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlaceCustomTraderActionTests extends CustomTraderTest {
    private Action action;
    private Player gm;
    private Item wand;
    private short actionId;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        action = mock(Action.class);
        actionId = ReflectionUtil.getPrivateField(customAction, PlaceCustomTraderAction.class.getDeclaredField("actionId"));
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        wand = factory.createNewItem(ItemList.wandGM);
    }

    // GetBehavioursFor

    @Test
    void testCorrectBehaviourReceived() {
        List<ActionEntry> entries = customAction.getBehavioursFor(gm, wand, 0, 0, true, 0);
        assertEquals(3, entries.size());
        assertEquals("Place Npc", entries.get(0).getActionString());
        assertEquals("Custom Trader", entries.get(1).getActionString());
        assertEquals("Currency Trader", entries.get(2).getActionString());
    }

    @Test
    void testPlayersDoNotGetOption() {
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        List<ActionEntry> entries = customAction.getBehavioursFor(player, wand, 0, 0, true, 0);
        assertNull(entries);
    }

    @Test
    void testWandRequired() {
        Item item = factory.createNewItem();
        assert !item.isWand();
        List<ActionEntry> entries = customAction.getBehavioursFor(gm, item, 0, 0, true, 0);
        assertNull(entries);
    }

    // Action

    @Test
    void testQuestionReceivedCustomTrader() throws NoSuchFieldException, IllegalAccessException {
        when(action.getActionEntry()).thenReturn(new ActionEntry(actionId, "Custom Trader", "placing custom trader"));
        boolean result = customAction.action(action, gm, wand, 0, 0, true,  0, 0, actionId, 0f);
        assertTrue(result);
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new PlaceCustomTraderQuestion(gm, Objects.requireNonNull(Zones.getTileOrNull(0, 0, true)), 0).sendQuestion();

        // To account for random gender.
        String[] bml = factory.getCommunicator(gm).getBml();
        List<String> fixed = new ArrayList<>();
        for (String b : bml) {
            fixed.add(b.replace(";selected=\"true\"", ""));
        }
        ReflectionUtil.setPrivateField(factory.getCommunicator(gm), FakeCommunicator.class.getDeclaredField("bml"), fixed);

        assertThat(gm, Assert.bmlEqual());
    }

    @Test
    void testQuestionReceivedCurrencyTrader() throws NoSuchFieldException, IllegalAccessException {
        boolean result = currencyAction.action(action, gm, wand, 0, 0, true,  0, 0, currencyAction.getActionId(), 0f);
        assertTrue(result);
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new PlaceCurrencyTraderQuestion(gm, Objects.requireNonNull(Zones.getTileOrNull(0, 0, true)), 0).sendQuestion();

        // To account for random gender.
        String[] bml = factory.getCommunicator(gm).getBml();
        List<String> fixed = new ArrayList<>();
        for (String b : bml) {
            fixed.add(b.replace(";selected=\"true\"", ""));
        }
        ReflectionUtil.setPrivateField(factory.getCommunicator(gm), FakeCommunicator.class.getDeclaredField("bml"), fixed);

        assertThat(gm, Assert.bmlEqual());
    }

    @Test
    void testPlayersDoNotReceiveBML() {
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        boolean result = customAction.action(action, player, wand, 0, 0, true, 0, 0, actionId, 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testWandRequiredForBML() {
        Item item = factory.createNewItem();
        assert !item.isWand();
        boolean result = customAction.action(action, gm, item, 0, 0, true, 0, 0, actionId, 0f);
        assertFalse(result);
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testIncorrectTileInformation() {
        boolean result = customAction.action(action, gm, wand, -250, -250, true, 0, 0, actionId, 0f);
        assertTrue(result);
        assertEquals(1, factory.getCommunicator(gm).getMessages().length);
        assertThat(gm, receivedMessageContaining("not be located"));
    }
}
