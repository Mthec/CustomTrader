package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.PlaceCurrencyTraderQuestion;
import com.wurmonline.server.questions.PlaceCustomTraderQuestion;
import com.wurmonline.server.questions.PlaceStatTraderQuestion;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.Assert;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
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
    private String actionString;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        action = mock(Action.class);
        when(action.getActionString()).thenAnswer(i -> actionString);
        actionId = getActionId(1);
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        wand = factory.createNewItem(ItemList.wandGM);
    }

    private short getActionId(int idx) {
        try {
            return ReflectionUtil.<List<ActionEntry>>getPrivateField(null, PlaceNpcMenu.class.getDeclaredField("actionEntries")).get(idx).getNumber();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // GetBehavioursFor

    @Test
    void testCorrectBehaviourReceived() {
        List<ActionEntry> entries = menu.getBehavioursFor(gm, wand, 0, 0, true, 0);
        assertEquals(4, entries.size());
        assertEquals("Place Npc", entries.get(0).getActionString());
        assertEquals("Custom Trader", entries.get(1).getActionString());
        assertEquals("Currency Trader", entries.get(2).getActionString());
        assertEquals("Stat Trader", entries.get(3).getActionString());
    }

    @Test
    void testPlayersDoNotGetOption() {
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        List<ActionEntry> entries = menu.getBehavioursFor(player, wand, 0, 0, true, 0);
        assertNull(entries);
    }

    @Test
    void testWandRequired() {
        Item item = factory.createNewItem();
        assert !item.isWand();
        List<ActionEntry> entries = menu.getBehavioursFor(gm, item, 0, 0, true, 0);
        assertNull(entries);
    }

    // Action

    @Test
    void testQuestionReceivedCustomTrader() throws NoSuchFieldException, IllegalAccessException {
        actionString = "Custom Trader";
        assertTrue(menu.action(action, gm, wand, 0, 0, true,  0, 0, actionId, 0f));
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
        actionString = "Currency Trader";
        assertTrue(menu.action(action, gm, wand, 0, 0, true,  0, 0, getActionId(2), 0f));
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
    void testQuestionReceivedStatTrader() throws NoSuchFieldException, IllegalAccessException {
        actionString = "Stat Trader";
        assertTrue(menu.action(action, gm, wand, 0, 0, true,  0, 0, getActionId(3), 0f));
        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        new PlaceStatTraderQuestion(gm, Objects.requireNonNull(Zones.getTileOrNull(0, 0, true)), 0).sendQuestion();

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
        actionString = "Custom Trader";
        Player player = factory.createNewPlayer();
        assert player.getPower() < 2;
        assertTrue(menu.action(action, player, wand, 0, 0, true, 0, 0, actionId, 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testWandRequiredForBML() {
        actionString = "Custom Trader";
        Item item = factory.createNewItem();
        assert !item.isWand();
        assertTrue(menu.action(action, gm, item, 0, 0, true, 0, 0, actionId, 0f));
        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }

    @Test
    void testIncorrectTileInformation() {
        actionString = "Custom Trader";
        assertTrue(menu.action(action, gm, wand, -250, -250, true, 0, 0, actionId, 0f));
        assertEquals(1, factory.getCommunicator(gm).getMessages().length);
        assertThat(gm, receivedMessageContaining("not be located"));
    }
}
