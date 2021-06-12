package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Items;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.economy.CustomTraderEconomy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.constants.CreatureTypes;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import org.gotti.wurmunlimited.modsupport.CreatureTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreature;

public class StatTraderTemplate implements ModCreature {
    private static int templateId;

    @Override
    public CreatureTemplateBuilder createCreateTemplateBuilder() {
        int[] types = new int[] {
                CreatureTypes.C_TYPE_INVULNERABLE,
                CreatureTypes.C_TYPE_HUMAN
        };

        CreatureTemplateBuilder trader = new CreatureTemplateBuilder(
                "mod.creature.stattrader",
                "stat trader",
                "An envoy from the king, selling authorised items in exchange for a stat (favour, health, karma).",
                "model.creature.humanoid.human.salesman",
                types,
                (byte)0,
                (short)2,
                MiscConstants.SEX_MALE,
                (short)180,
                (short)20,
                (short)35,
                "sound.death.male",
                "sound.death.female",
                "sound.combat.hit.male",
                "sound.combat.hit.female",
                1.0f,
                1.0f,
                2.0f,
                0f,
                0f,
                0f,
                0.8f,
                0,
                new int[0],
                3,
                0,
                ItemMaterials.MATERIAL_MEAT_HUMAN
        );

        trader.skill(102, 15.0F);
        trader.skill(104, 15.0F);
        trader.skill(103, 10.0F);
        trader.skill(100, 30.0F);
        trader.skill(101, 30.0F);
        trader.skill(105, 99.0F);
        trader.skill(106, 4.0F);
        trader.skill(10052, 40.0F);
        trader.baseCombatRating(70.0f);
        trader.hasHands(true);

        templateId = trader.getTemplateId();

        return trader;
    }

    public static boolean is(Creature creature) {
        return creature.getTemplateId() == templateId;
    }

    public static boolean is(CreatureTemplate template) {
        return template.getTemplateId() == templateId;
    }

    public static Creature createNewTrader(VolaTile tile, int floorLevel, String name, byte sex, byte kingdom, Stat stat, String tag) throws Exception {
        Creature trader = Creature.doNew(templateId, (float)(tile.getTileX() << 2) + 2.0F, (float)(tile.getTileY() << 2) + 2.0F, 180.0F, tile.getLayer(), name, sex, kingdom);

        if (floorLevel != 0) {
            trader.pushToFloorLevel(floorLevel);
        }

        CustomTraderDatabase.addNew(trader, stat, tag);
        CustomTraderEconomy.createShop(trader.getWurmId());

        // Cleaning up after Shop.createShop
        for (Item item : trader.getInventory().getItemsAsArray()) {
            Items.destroyItem(item.getWurmId());
        }

        if (!tag.isEmpty()) {
            CustomTraderDatabase.fullyStock(trader);
        }

        return trader;
    }
}
