package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.Currency;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AdvancedCurrencyQuestionTests extends CustomTraderTest {
    private Player gm;
    private Creature trader;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        trader = factory.createNewCurrencyTrader();
    }

    private Currency getCurrency() {
        try {
            Currency currency = new Currency(ItemTemplateFactory.getInstance().getTemplate(ItemList.log), 12, 34, Materials.MATERIAL_WOOD_CEDAR, (byte)2, false);
            CustomTraderDatabase.setCurrencyFor(trader, currency);
            return currency;
        } catch (NoSuchTemplateException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testValuesProperlySet() throws NoSuchTemplateException {
        Currency currency = getCurrency();
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).sendQuestion();

        assertThat(gm, receivedBMLContaining("log"));
        assertThat(gm, receivedBMLContaining("input{text=\"12\";id=\"min_ql\""));
        assertThat(gm, receivedBMLContaining("input{text=\"34\";id=\"exact_ql\""));
        assertThat(gm, receivedBMLContaining("default=\"" + (new EligibleMaterials(currency.getTemplate()).getIndexOf(Materials.MATERIAL_WOOD_CEDAR) + 1) + "\""));
        assertThat(gm, receivedBMLContaining("radio{group=\"rarity\";id=\"2\";text=\"Supreme\";selected=\"true\"}"));
        assertThat(gm, receivedBMLContaining("label{text=\"Require full weight?\"}checkbox{id=\"weight\";selected=\"false\""));
        assertThat(gm, receivedBMLContaining("any,"));
    }

    // answers
    
    private Properties getSameProperties(Currency currency) {
        Properties properties = new Properties();
        properties.setProperty("confirm", "true");
        properties.setProperty("min_ql", Float.toString(currency.minQL));
        properties.setProperty("exact_ql", Float.toString(currency.exactQL));
        properties.setProperty("mat", Byte.toString((byte)(new EligibleMaterials(currency.getTemplate()).getIndexOf(currency.material) + 1)));
        properties.setProperty("rarity", Byte.toString(currency.rarity));
        properties.setProperty("weight", Boolean.toString(currency.onlyFullWeight));
        return properties;
    }

    @Test
    void testBackSendsAppropriateQuestion() {
        Currency currency = getCurrency();

        Properties properties = new Properties();
        properties.setProperty("back", "true");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);
        new CurrencyTraderManagementQuestion(gm, trader).sendQuestion();

        assertThat(gm, bmlEqual());
    }

    @Test
    void testCancelDoesNothing() {
        Currency currency = getCurrency();

        Properties properties = new Properties();
        properties.setProperty("cancel", "true");
        properties.setProperty("min_ql", Float.toString(currency.minQL + 1));
        properties.setProperty("exact_ql", Float.toString(currency.exactQL + 1));
        properties.setProperty("mat", Byte.toString((byte)(currency.material + 1)));
        properties.setProperty("rarity", Byte.toString((byte)(currency.rarity + 1)));
        properties.setProperty("weight", "true");

        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testTemplateChanged() throws NoSuchTemplateException {
        Currency currency = getCurrency();
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(ItemList.barrelSmall);

        Properties properties = getSameProperties(currency);
        new AdvancedCurrencyQuestion(gm, trader, template, currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertNotEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testMinQLChanged() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("min_ql", Float.toString(99));
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(99, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testMinQLMissing() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.remove("min_ql");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("No Minimum"));
    }

    @Test
    void testMinQLEmptyIsAccepted() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("min_ql", "");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(-1, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testMinQLUnder0() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("min_ql", "-1");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("greater than 0"));
    }

    @Test
    void testMinQLOver100() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("min_ql", "101");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("cannot be greater than 100"));
    }

    @Test
    void testMinQLInvalid() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("min_ql", "abc");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("was invalid"));
    }

    @Test
    void testExactQLChanged() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("exact_ql", Float.toString(99));
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(99, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testExactQLMissing() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.remove("exact_ql");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("No Exact"));
    }

    @Test
    void testExactQLEmptyIsAccepted() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("exact_ql", "");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(-1, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testExactQLUnder0() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("exact_ql", "-1");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("greater than 0"));
    }

    @Test
    void testExactQLOver100() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("exact_ql", "101");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("cannot be greater than 100"));
    }

    @Test
    void testExactQLInvalid() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("exact_ql", "abc");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("was invalid"));
    }

    @Test
    void testMaterialChanged() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("mat", Integer.toString(new EligibleMaterials(currency.getTemplate()).getIndexOf(Materials.MATERIAL_WOOD_BIRCH) + 1));
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(Materials.MATERIAL_WOOD_BIRCH, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testMaterialChangedAny() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("mat", "0");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(-1, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testRarityChanged() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("rarity", Byte.toString((byte)1));
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals((byte)1, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testRarityChangedAny() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("rarity", Byte.toString((byte)-1));
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals((byte)-1, newCurrency.rarity);
        assertEquals(currency.onlyFullWeight, newCurrency.onlyFullWeight);
    }

    @Test
    void testOnlyFullWeightChanged() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.setProperty("weight", "true");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        Currency newCurrency = CustomTraderDatabase.getCurrencyFor(trader);
        assertNotNull(newCurrency);
        assertEquals(currency.templateId, newCurrency.templateId);
        assertEquals(currency.minQL, newCurrency.minQL);
        assertEquals(currency.exactQL, newCurrency.exactQL);
        assertEquals(currency.material, newCurrency.material);
        assertEquals(currency.rarity, newCurrency.rarity);
        assertTrue(newCurrency.onlyFullWeight);
    }

    @Test
    void testOnlyFullWeightMissing() {
        Currency currency = getCurrency();

        Properties properties = getSameProperties(currency);
        properties.remove("weight");
        new AdvancedCurrencyQuestion(gm, trader, currency.getTemplate(), currency).answer(properties);

        assertEquals(currency, CustomTraderDatabase.getCurrencyFor(trader));
        assertThat(gm, receivedMessageContaining("Weight was invalid"));
    }
}
