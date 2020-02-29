package com.wurmonline.server.questions;

import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CustomTraderEditTagsTests extends CustomTraderTest {
    private Player gm;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        gm = factory.createNewPlayer();
    }

    @Test
    void testTagsAppearInList() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        new CustomTraderEditTags(gm).sendQuestion();

        assertThat(gm, receivedBMLContaining("tag1"));
        assertThat(gm, receivedBMLContaining("tag2"));
        assertThat(gm, receivedBMLContaining("tag3"));
    }

    // answer

    @Test
    void testNoTags() {
        assertDoesNotThrow(() -> new CustomTraderEditTags(gm).answer(new Properties()));
    }

    @Test
    void testRemoveSingleTag() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("remove", "true");
        new CustomTraderEditTags(gm).answer(properties);

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(2, allTags.size());
        assertFalse(allTags.contains("tag1"));
        assertThat(gm, receivedMessageContaining("1 tag was removed"));
    }

    @Test
    void testRemoveMultipleTags() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("s2", "true");
        properties.setProperty("remove", "true");
        new CustomTraderEditTags(gm).answer(properties);

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(1, allTags.size());
        assertTrue(allTags.contains("tag2"));
        assertThat(gm, receivedMessageContaining("2 tags were removed"));
    }

    @Test
    void testNotRemovedIfNoneSelected() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("remove", "true");
        new CustomTraderEditTags(gm).answer(properties);

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, receivedMessageContaining("0 tags were removed"));
    }

    @Test
    void testRenameSingleSelected() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("rename", "true");
        new CustomTraderEditTags(gm).answer(properties);
        new CustomTraderRenameTags(gm, Collections.singletonList("tag1")).sendQuestion();

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, bmlEqual());
        assertEquals(0, factory.getCommunicator(gm).getMessages().length);
    }

    @Test
    void testRenameMultipleSelected() throws CustomTraderDatabase.FailedToUpdateTagException {
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag1");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag2");
        CustomTraderDatabase.updateTag(factory.createNewCustomTrader(), "tag3");

        Properties properties = new Properties();
        properties.setProperty("s0", "true");
        properties.setProperty("s2", "true");
        properties.setProperty("rename", "true");
        new CustomTraderEditTags(gm).answer(properties);
        new CustomTraderRenameTags(gm, Arrays.asList("tag1", "tag3")).sendQuestion();

        List<String> allTags = CustomTraderDatabase.getAllTags();
        assertEquals(3, allTags.size());
        assertTrue(allTags.contains("tag1"));
        assertTrue(allTags.contains("tag2"));
        assertTrue(allTags.contains("tag3"));
        assertThat(gm, bmlEqual());
        assertEquals(0, factory.getCommunicator(gm).getMessages().length);
    }
}
