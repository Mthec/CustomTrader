package com.wurmonline.server.questions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ALL")
public class FilterTests {
    private final String actual = "Wurm Unlimited";
    private final String empty = "";
    private final String whole = actual;
    private final String lowerCase = actual.toLowerCase();
    private final String upperCase = actual.toUpperCase();
    private final String skipStart = "asdf" + actual;
    private final String skipEnd = actual + "asdf";
    private final String skipBoth = "asdf" + actual + "asdf";
    private final String wrong = "Minecraft";

    private final Filter text = new Filter(actual);
    private final Filter wildcard = new Filter("*urm Unlimite*");
    private final Filter wildStart = new Filter("*urm Unlimited");
    private final Filter wildEnd = new Filter("Wurm Unlimite*");

    @Test
    void testEmptyMatches() {
        Filter filter = new Filter(empty);
        assertTrue(filter.matches(whole));
        assertTrue(filter.matches(skipStart));
        assertTrue(filter.matches(skipEnd));
        assertTrue(filter.matches(skipBoth));
        assertTrue(filter.matches(wrong));
    }

    @Test
    void testWholeMatches() {
        assertTrue(text.matches(whole));
        assertTrue(wildcard.matches(whole));
        assertTrue(wildStart.matches(whole));
        assertTrue(wildEnd.matches(whole));
    }

    @Test
    void testLowerCaseMatches() {
        assertTrue(text.matches(lowerCase));
        assertTrue(wildcard.matches(lowerCase));
        assertTrue(wildStart.matches(lowerCase));
        assertTrue(wildEnd.matches(lowerCase));
    }

    @Test
    void testUpperCaseMatches() {
        assertTrue(text.matches(upperCase));
        assertTrue(wildcard.matches(upperCase));
        assertTrue(wildStart.matches(upperCase));
        assertTrue(wildEnd.matches(upperCase));
    }

    @Test
    void testSkipStartMatches() {
        assertFalse(text.matches(skipStart));
        assertTrue(wildcard.matches(skipStart));
        assertTrue(wildStart.matches(skipStart));
        assertFalse(wildEnd.matches(skipStart));
    }

    @Test
    void testSkipEndMatches() {
        assertFalse(text.matches(skipEnd));
        assertTrue(wildcard.matches(skipEnd));
        assertFalse(wildStart.matches(skipEnd));
        assertTrue(wildEnd.matches(skipEnd));
    }

    @Test
    void testSkipBothMatches() {
        assertFalse(text.matches(skipBoth));
        assertTrue(wildcard.matches(skipBoth));
        assertFalse(wildStart.matches(skipBoth));
        assertFalse(wildEnd.matches(skipBoth));
    }

    @Test
    void testWrongMatches() {
        assertFalse(text.matches(wrong));
        assertFalse(wildcard.matches(wrong));
        assertFalse(wildStart.matches(wrong));
        assertFalse(wildEnd.matches(wrong));
    }
}
