package com.wurmonline.server.questions;

import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.npcs.customtrader.CustomTraderMod;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceOrManageTraderQuestionTests extends CustomTraderTest {
    private Player gm;
    private PlaceOrManageTraderQuestion question;
    
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        gm = factory.createNewPlayer();
        new CustomTraderMod();
        question = new PlaceOrManageTraderQuestion(gm, "", 123L) {
            @Override
            public void answer(Properties answers) {
                setAnswer(answers);
            }

            @Override
            public void sendQuestion() {

            }
        };
    }
    
    // Place Trader

    @Test
    void testSetName() {
        String name = StringUtilities.raiseFirstLetter("MyName");
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", name);
        question.answer(properties);

        assertEquals(name, question.getName((byte)1));
    }

    @Test
    void testSetNameIllegalCharacters() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "%Name");
        question.answer(properties);

        question.getName((byte)1);
        assertThat(gm, receivedMessageContaining("The trader didn't like that name"));
    }

    @Test
    void testSetNameRandomWhenBlank() {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("name", "");
        question.answer(properties);

        question.getName((byte)1);
        assertThat(gm, receivedMessageContaining("The trader chose a new name"));
    }
}
