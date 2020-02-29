package mod.wurmunlimited.npcs.customtrader.stock;

import com.wurmonline.server.items.Item;

import java.util.Arrays;

public class StockItem {
    public final int templateId;
    public final float ql;
    public final int price;
    public final byte material;
    public final byte rarity;
    public final Enchantment[] enchantments;

    public StockItem(int templateId, float ql, int price, byte material, byte rarity, Enchantment[] enchantments) {
        this.templateId = templateId;
        this.ql = ql;
        this.price = price;
        this.material = material;
        this.rarity = rarity;
        this.enchantments = enchantments;
    }

    public boolean matches(Item item) {
        return item.getTemplateId() == templateId &&
                       item.getQualityLevel() == ql &&
                       item.getMaterial() == material &&
                       item.getRarity() == rarity &&
                       Arrays.equals(Enchantment.parseEnchantments(item), enchantments);
    }

    @Override
    public int hashCode() {
        int result = 3;
        result = 31 * result + templateId;
        result = 31 * result + Float.floatToIntBits(ql);
        result = 31 * result + price;
        result = 31 * result + (int)material;
        result = 31 * result + (int)rarity;
        result = 31 * result + Arrays.hashCode(enchantments);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StockItem) {
            StockItem item = (StockItem)obj;
            return item.templateId == templateId &&
                   item.ql == ql &&
                   item.price == price &&
                   item.material == material &&
                   item.rarity == rarity &&
                   Arrays.equals(item.enchantments, enchantments);
        }

        return super.equals(obj);
    }
}
