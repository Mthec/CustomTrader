package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CustomTraderTradeHandler;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.items.Trade;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stock.Enchantment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

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
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);

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
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);

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
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);
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
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);
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
        CustomTraderDatabase.addStockItemTo(customTrader, num, num, num, b, b, new Enchantment[0], num, num, 0);
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
}
