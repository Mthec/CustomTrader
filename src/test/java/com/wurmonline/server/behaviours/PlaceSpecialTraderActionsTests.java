package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.customtrader.CustomTraderTest;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PlaceSpecialTraderActionsTests extends CustomTraderTest {
    private Player gm;
    private Item wand;
    
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        wand = factory.createNewItem(ItemList.wandDeity);
    } 
    
    @Test
    void testActionEntriesSentOncePerGroup() {
        assertNotNull(customAction.getBehavioursFor(gm, wand, 1, 1, true, 1));
        assertNull(currencyAction.getBehavioursFor(gm, wand, 1, 1, true, 1));
        assertNotNull(currencyAction.getBehavioursFor(gm, wand, 1, 1, true, 1));
        assertNull(customAction.getBehavioursFor(gm, wand, 1, 1, true, 1));
    }
}
