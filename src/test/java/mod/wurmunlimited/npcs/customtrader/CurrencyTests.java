package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.items.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyTests extends CustomTraderTest {
    private Currency currency;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        currency = new Currency(ItemTemplateFactory.getInstance().getTemplate(ItemList.eggEaster), -1, -1, (byte)-1, (byte)-1, false);
    }

    @Test
    void testEquals() throws NoSuchTemplateException {
        Currency two = new Currency(currency.templateId, currency.minQL, currency.exactQL, currency.material, currency.rarity, currency.onlyFullWeight);

        assertEquals(currency, two);
    }

    @Test
    void testMatchesWrongTemplate() throws NoSuchTemplateException {
        Item item = factory.createNewItem(currency);
        assertEquals(Currency.MatchStatus.WRONG_TEMPLATE, new Currency(currency.templateId + 1, -1, -1, (byte)-1, (byte)-1, false).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testMatchesNotExactQL() {
        Item item = factory.createNewItem(currency);
        item.setQualityLevel(10);
        assertEquals(Currency.MatchStatus.QL_DOES_NOT_MATCH_EXACT, new Currency(currency.getTemplate(), -1, 11, (byte)-1, (byte)-1, false).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testMatchesBelowMinQL() {
        Item item = factory.createNewItem(currency);
        item.setQualityLevel(9.9f);
        assertEquals(Currency.MatchStatus.QL_TOO_LOW, new Currency(currency.getTemplate(), 10, -1, (byte)-1, (byte)-1, false).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testMatchesWrongMaterial() {
        Item item = factory.createNewItem(currency);
        item.setMaterial(Materials.MATERIAL_GOLD);
        assertEquals(Currency.MatchStatus.WRONG_MATERIAL, new Currency(currency.getTemplate(), -1, -1, Materials.MATERIAL_SILVER, (byte)-1, false).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testMatchesWrongRarity() {
        Item item = factory.createNewItem(currency);
        item.setRarity((byte)3);
        assertEquals(Currency.MatchStatus.WRONG_RARITY, new Currency(currency.getTemplate(), -1, -1, (byte)-1, (byte)2, false).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testMatchesNotFullWeight() {
        Item item = factory.createNewItem(currency);
        item.setWeight(1, false);
        assertEquals(Currency.MatchStatus.DOES_NOT_WEIGH_ENOUGH, new Currency(currency.getTemplate(), -1, -1, (byte)-1, (byte)-1, true).matches(item));
        assertEquals(Currency.MatchStatus.MATCHES, currency.matches(item));
    }

    @Test
    void testGetNameFor() {
        assertEquals("easter eggs", currency.getNameFor(0));
        assertEquals("easter egg", currency.getNameFor(1));
        assertEquals("easter eggs", currency.getNameFor(2));
    }

    @Test
    void testGetName() {
        assertEquals("easter egg", currency.getName());
        assertEquals("easter egg of exactly 2ql", new Currency(currency.getTemplate(), -1, 2, (byte)-1, (byte)-1, false).getName());
        assertEquals("easter egg of at least 2ql", new Currency(currency.getTemplate(), 2, -1, (byte)-1, (byte)-1, false).getName());
        assertEquals("bronze easter egg", new Currency(currency.getTemplate(), -1, -1, Materials.MATERIAL_BRONZE, (byte)-1, false).getName());
        assertEquals("fantastic easter egg", new Currency(currency.getTemplate(), -1, -1, (byte)-1, (byte)3, false).getName());
        assertEquals("rare brass easter egg of exactly 12ql", new Currency(currency.getTemplate(), -1, 12, Materials.MATERIAL_BRASS, (byte)1, false).getName());
    }

    @Test
    void testGetPlural() {
        assertEquals("easter eggs", currency.getPlural());
        assertEquals("easter eggs of exactly 2ql", new Currency(currency.getTemplate(), -1, 2, (byte)-1, (byte)-1, false).getPlural());
        assertEquals("easter eggs of at least 2ql", new Currency(currency.getTemplate(), 2, -1, (byte)-1, (byte)-1, false).getPlural());
        assertEquals("bronze easter eggs", new Currency(currency.getTemplate(), -1, -1, Materials.MATERIAL_BRONZE, (byte)-1, false).getPlural());
        assertEquals("fantastic easter eggs", new Currency(currency.getTemplate(), -1, -1, (byte)-1, (byte)3, false).getPlural());
        assertEquals("rare brass easter eggs of exactly 12ql", new Currency(currency.getTemplate(), -1, 12, Materials.MATERIAL_BRASS, (byte)1, false).getPlural());
    }
}
