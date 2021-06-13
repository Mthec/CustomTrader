package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.Question;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import mod.wurmunlimited.npcs.customtrader.stock.StockInfo;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomTraderModTests extends CustomTraderTest {
    private Creature customTrader;
    private Creature normalTrader;
    private final int num = 5;
    private final byte b = 0;
    private int normalItemCount;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        customTrader = factory.createNewCustomTrader();
        normalTrader = factory.createNewTrader();
        normalItemCount = normalTrader.getInventory().getItemCount();
    }

    @Test
    void testPollAlive() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);

        InvocationHandler handler = new CustomTraderMod()::poll;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(false);
        Object[] args = new Object[0];

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(false, handler.invoke(customTrader, method, args));
        assertEquals(num, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrader, args);

        assertEquals(false, handler.invoke(normalTrader, method, args));
        assertEquals(normalItemCount, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrader, args);
    }

    @Test
    void testPollDead() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);

        InvocationHandler handler = new CustomTraderMod()::poll;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(true);
        Object[] args = new Object[0];

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(true, handler.invoke(customTrader, method, args));
        assertEquals(0, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrader, args);

        assertEquals(true, handler.invoke(normalTrader, method, args));
        assertEquals(normalItemCount, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrader, args);
    }

    @Test
    void testPollItemsOnCustomTraderPreventDecay() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, ItemList.casserole, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);
        CustomTraderDatabase.restock(customTrader);

        CustomTraderMod mod = new CustomTraderMod();
        ReflectionUtil.setPrivateField(mod, CustomTraderMod.class.getDeclaredField("preventDecay"), true);
        InvocationHandler handler = mod::pollOwned;
        Method method = mock(Method.class);
        when(method.invoke(any(Item.class), any())).thenAnswer((Answer<Boolean>)i -> {
            Item first = ((Item)i.getArgument(0)).getFirstContainedItem();
            if (first != null)
                first.setDamage(50);
            return true;
        });
        Object[] args = new Object[] { customTrader };
        Object[] args2 = new Object[] { normalTrader };

        assert customTrader.getInventory().getItems().size() != 0;
        assertEquals(false, handler.invoke(customTrader.getInventory(), method, args));
        verify(method, never()).invoke(customTrader.getInventory(), args);
        assertEquals(0, customTrader.getInventory().getFirstContainedItem().getDamage());

        assertEquals(true, handler.invoke(normalTrader.getInventory(), method, args2));
        verify(method, times(1)).invoke(normalTrader.getInventory(), args2);
    }

    @Test
    void testPollItemsOnCustomTraderNotPreventDecay() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, ItemList.casserole, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);
        CustomTraderDatabase.restock(customTrader);

        CustomTraderMod mod = new CustomTraderMod();
        ReflectionUtil.setPrivateField(mod, CustomTraderMod.class.getDeclaredField("preventDecay"), false);
        InvocationHandler handler = mod::pollOwned;
        Method method = mock(Method.class);
        when(method.invoke(any(Item.class), any())).thenAnswer((Answer<Boolean>)i -> {
            Item first = ((Item)i.getArgument(0)).getFirstContainedItem();
            if (first != null)
                first.setDamage(50);
            return true;
        });
        Object[] args = new Object[] { customTrader };
        Object[] args2 = new Object[] { normalTrader };

        assert customTrader.getInventory().getItems().size() != 0;
        assertEquals(true, handler.invoke(customTrader.getInventory(), method, args));
        verify(method, times(1)).invoke(customTrader.getInventory(), args);
        assertEquals(50, customTrader.getInventory().getFirstContainedItem().getDamage());

        assertEquals(true, handler.invoke(normalTrader.getInventory(), method, args2));
        verify(method, times(1)).invoke(normalTrader.getInventory(), args2);
    }

    @Test
    void testMakeTradeComplete() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);
        customTrader.getShop().setMoney(100);
        normalTrader.getShop().setMoney(100);

        InvocationHandler handler = new CustomTraderMod()::makeTrade;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(true);
        Object[] args = new Object[0];
        Trade customTrade = new Trade(factory.createNewPlayer(), customTrader);
        Trade normalTrade = new Trade(factory.createNewPlayer(), normalTrader);

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(true, handler.invoke(customTrade, method, args));
        assertEquals(num, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrade, args);
        assertEquals(0, customTrader.getShop().getMoney());

        assertEquals(true, handler.invoke(normalTrade, method, args));
        assertEquals(normalItemCount, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrade, args);
        assertEquals(100, normalTrader.getShop().getMoney());
    }

    @Test
    void testMakeTradeNotComplete() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], (byte)0, num, num, 0);
        customTrader.getShop().setMoney(100);
        normalTrader.getShop().setMoney(100);

        InvocationHandler handler = new CustomTraderMod()::makeTrade;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(false);
        Object[] args = new Object[0];
        Trade customTrade = new Trade(factory.createNewPlayer(), customTrader);
        Trade normalTrade = new Trade(factory.createNewPlayer(), normalTrader);

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(false, handler.invoke(customTrade, method, args));
        assertEquals(0, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrade, args);
        assertEquals(100, customTrader.getShop().getMoney());

        assertEquals(false, handler.invoke(normalTrade, method, args));
        assertEquals(normalItemCount, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrade, args);
        assertEquals(100, normalTrader.getShop().getMoney());
    }

    @Test
    void testGetTradeHandler() throws Throwable {
        Creature currencyTrader = factory.createNewCurrencyTrader();
        Creature statTrader = factory.createNewStatTrader();

        InvocationHandler handler = new CustomTraderMod()::getTradeHandler;
        Method method = mock(Method.class);
        Object[] args = new Object[0];
        customTrader.setTrade(new Trade(factory.createNewPlayer(), customTrader));
        currencyTrader.setTrade(new Trade(factory.createNewPlayer(), currencyTrader));
        statTrader.setTrade(new Trade(factory.createNewPlayer(), statTrader));
        normalTrader.setTrade(new Trade(factory.createNewPlayer(), normalTrader));

        assert customTrader.getInventory().getItems().size() == 0;
        assertTrue(handler.invoke(customTrader, method, args) instanceof CustomTraderTradeHandler);
        verify(method, never()).invoke(customTrader, args);
        assertTrue(handler.invoke(currencyTrader, method, args) instanceof CurrencyTraderTradeHandler);
        verify(method, never()).invoke(currencyTrader, args);
        assertTrue(handler.invoke(statTrader, method, args) instanceof StatTraderTradeHandler);
        verify(method, never()).invoke(statTrader, args);

        assertNull(handler.invoke(normalTrader, method, args));
        verify(method, times(1)).invoke(normalTrader, args);
    }

    private void checkCreatureCreation(Creature gm, InvocationHandler handler, Method method, Object[] args, int templateId) throws Throwable {
        String name = "Name";
        int templateIndex = -1;
        int i = 0;
        //noinspection unchecked
        for (CreatureTemplate template : ((List<CreatureTemplate>)ReflectionUtil.getPrivateField(args[0], CreatureCreationQuestion.class.getDeclaredField("cretemplates")))) {
            if (template.getTemplateId() == templateId) {
                templateIndex = i;
                break;
            }
            ++i;
        }
        assert templateIndex != -1;
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(i));
        answers.setProperty("cname", name);
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }
        assert factory.getAllCreatures().size() == 0;

        assertNull(handler.invoke(null, method, args));
        verify(method, never()).invoke(null, args);
        assertEquals(1, factory.getAllCreatures().size());
        Creature customTrader = factory.getAllCreatures().iterator().next();
        assertEquals(templateId, customTrader.getTemplateId());
        assertEquals("Trader_" + name, customTrader.getName());
        assertEquals((byte)1, customTrader.getSex());
        assertEquals(gm.isOnSurface(), customTrader.isOnSurface());
        assertEquals(0, customTrader.getInventory().getItems().size());
        assertThat(gm, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    void testCreatureCreation() throws Throwable {
        Player gm = factory.createNewPlayer();
        Item wand = factory.createNewItem(ItemList.wandGM);
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = new CustomTraderMod()::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(gm, "", "", wand.getWurmId(), tileX, tileY, -1, -10) };
        ((CreatureCreationQuestion)args[0]).sendQuestion();

        checkCreatureCreation(gm, handler, method, args, ReflectionUtil.getPrivateField(null, CustomTraderTemplate.class.getDeclaredField("templateId")));
        checkCreatureCreation(gm, handler, method, args, ReflectionUtil.getPrivateField(null, CurrencyTraderTemplate.class.getDeclaredField("templateId")));
        checkCreatureCreation(gm, handler, method, args, ReflectionUtil.getPrivateField(null, StatTraderTemplate.class.getDeclaredField("templateId")));
    }

    @Test
    void testNonCustomTraderCreatureCreation() throws Throwable {
        Player gm = factory.createNewPlayer();
        Item wand = factory.createNewItem(ItemList.wandGM);
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = new CustomTraderMod()::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(gm, "", "", wand.getWurmId(), tileX, tileY, -1, -10)};
        ((CreatureCreationQuestion)args[0]).sendQuestion();
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(0));
        answers.setProperty("cname", "MyName");
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }

        assertNull(handler.invoke(null, method, args));
        verify(method, times(1)).invoke(null, args);
        assertEquals(0, factory.getAllCreatures().size());
        assertThat(gm, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    void testOnPlayerMessageRestock() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {
            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock " + OnPlayerMessageReceiver.tag, "Local"));
            assertThat(gm, receivedMessageContaining("were restocked"));

            r.assertCount(5);
        });
    }

    @Test
    void testOnPlayerMessageRestockNoTraders() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {

            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock blah", "Local"));
            assertThat(gm, receivedMessageContaining("no traders"));

            r.assertCount(0);
        });
    }

    @Test
    void testOnPlayerMessageRestockNeedTag() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {
            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock", "Local"));
            assertThat(gm, receivedMessageContaining("need to provide a tag"));

            r.assertCount(0);
        });
    }

    @Test
    void testOnPlayerMessageRestockNoStock() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {
            StockInfo[] stockInfo = CustomTraderDatabase.getStockFor(r.traderTagged);
            try {
                for (StockInfo stock : stockInfo) {
                    CustomTraderDatabase.removeStockItemFrom(r.traderTagged, stock);
                }
            } catch (CustomTraderDatabase.StockUpdateException e) {
                throw new RuntimeException(e);
            }

            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock " + OnPlayerMessageReceiver.tag, "Local"));
            assertThat(gm, receivedMessageContaining("no stock"));

            r.assertCount(0);
        });
    }

    @Test
    void testOnPlayerMessageNoRepeatRestock() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {
            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock " + OnPlayerMessageReceiver.tag, "Local"));
            assertThat(gm, receivedMessageContaining("were restocked"));

            r.assertCount(5);

            r.traderTagged.getInventory().removeAndEmpty();

            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock " + OnPlayerMessageReceiver.tag, "Local"));
            assertThat(gm, receivedMessageContaining("need to wait"));

            r.assertCount(0);
        });
    }

    @Test
    void testOnPlayerMessageRestockMultiple() throws Throwable {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        new OnPlayerMessageReceiver(r -> {
            Creature anotherTraderTagged = factory.createNewCustomTrader(OnPlayerMessageReceiver.tag);
            r.addStockTo(anotherTraderTagged);

            assertEquals(MessagePolicy.DISCARD, r.mod.onPlayerMessage(gm.getCommunicator(), "/restock " + OnPlayerMessageReceiver.tag, "Local"));
            assertThat(gm, receivedMessageContaining("were restocked"));

            r.assertCount(5);
            assertEquals(5, anotherTraderTagged.getInventory().getItemCount());
        });
    }

    @Test
    void testTraderTagsCommandUsed() throws IOException {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);

        String tag1 = "t";
        String tag2 = "short_tag";
        String tag3 = "12345678901234567890";
        factory.createNewCustomTrader(tag1);
        factory.createNewCustomTrader(tag2);
        factory.createNewCustomTrader(tag3);

        new CustomTraderMod().onPlayerMessage(gm.getCommunicator(), "/tradertags", "Local");

        assertThat(gm, receivedMessageContaining("available - " + tag3 + ", " + tag2 + ", " + tag1 + "."));
    }

    @Test
    void testListTraderTags() throws IOException {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);

        String tag1 = "t";
        String tag2 = "short_tag";
        String tag3 = "12345678901234567890";
        factory.createNewCustomTrader(tag1);
        factory.createNewCustomTrader(tag2);
        factory.createNewCustomTrader(tag3);

        CustomTraderMod.listTraderTags(gm);

        assertThat(gm, receivedMessageContaining("available - " + tag3 + ", " + tag2 + ", " + tag1 + "."));
    }

    @Test
    void testListTraderTagsNoneSet() throws IOException {
        assert CustomTraderDatabase.getAllTags().isEmpty();
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)2);

        CustomTraderMod.listTraderTags(gm);

        assertThat(gm, receivedMessageContaining("no tags"));
    }
}
