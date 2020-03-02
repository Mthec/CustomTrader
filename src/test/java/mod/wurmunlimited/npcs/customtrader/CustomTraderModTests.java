package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CustomTraderTradeHandler;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.Question;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomTraderModTests extends CustomTraderTest {
    private Creature customTrader;
    private Creature normalTrader;
    private final int num = 5;
    private final byte b = 0;
    private int normalInventory;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        customTrader = factory.createNewCustomTrader();
        normalTrader = factory.createNewTrader();
        normalInventory = normalTrader.getInventory().getItemCount();
    }

    @Test
    void testPollAlive() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], num, num, 0);

        InvocationHandler handler = new CustomTraderMod()::poll;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(false);
        Object[] args = new Object[0];

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(false, handler.invoke(customTrader, method, args));
        assertEquals(num, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrader, args);

        assertEquals(false, handler.invoke(normalTrader, method, args));
        assertEquals(normalInventory, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrader, args);
    }

    @Test
    void testPollDead() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], num, num, 0);

        InvocationHandler handler = new CustomTraderMod()::poll;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(true);
        Object[] args = new Object[0];

        assert customTrader.getInventory().getItems().size() == 0;
        assertEquals(true, handler.invoke(customTrader, method, args));
        assertEquals(0, customTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(customTrader, args);

        assertEquals(true, handler.invoke(normalTrader, method, args));
        assertEquals(normalInventory, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrader, args);
    }

    @Test
    void testMakeTradeComplete() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], num, num, 0);
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
        assertEquals(normalInventory, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrade, args);
        assertEquals(100, normalTrader.getShop().getMoney());
    }

    @Test
    void testMakeTradeNotComplete() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], num, num, 0);
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
        assertEquals(normalInventory, normalTrader.getInventory().getItems().size());
        verify(method, times(1)).invoke(normalTrade, args);
        assertEquals(100, normalTrader.getShop().getMoney());
    }

    @Test
    void testGetTradeHandler() throws Throwable {
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, num, new Enchantment[0], num, num, 0);
        customTrader.getShop().setMoney(100);
        normalTrader.getShop().setMoney(100);

        InvocationHandler handler = new CustomTraderMod()::getTradeHandler;
        Method method = mock(Method.class);
        Object[] args = new Object[0];
        customTrader.setTrade(new Trade(factory.createNewPlayer(), customTrader));
        normalTrader.setTrade(new Trade(factory.createNewPlayer(), normalTrader));

        assert customTrader.getInventory().getItems().size() == 0;
        assertTrue(handler.invoke(customTrader, method, args) instanceof CustomTraderTradeHandler);
        verify(method, never()).invoke(customTrader, args);

        assertNull(handler.invoke(normalTrader, method, args));
        verify(method, times(1)).invoke(normalTrader, args);
    }

    @Test
    void testCreatureCreation() throws Throwable {
        Player gm = factory.createNewPlayer();
        Item wand = factory.createNewItem(ItemList.wandGM);
        String name = "Name";
        int tileX = 250;
        int tileY = 250;
        int templateId = ReflectionUtil.getPrivateField(null, CustomTraderTemplate.class.getDeclaredField("templateId"));

        InvocationHandler handler = new CustomTraderMod()::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(gm, "", "", wand.getWurmId(), tileX, tileY, -1, -10)};
        ((CreatureCreationQuestion)args[0]).sendQuestion();
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
        assertEquals("Trader_" + name, customTrader.getName());
        assertEquals((byte)1, customTrader.getSex());
        assertEquals(gm.isOnSurface(), customTrader.isOnSurface());
        assertEquals(0, customTrader.getInventory().getItems().size());
        assertThat(gm, didNotReceiveMessageContaining("An error occurred"));
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
}
