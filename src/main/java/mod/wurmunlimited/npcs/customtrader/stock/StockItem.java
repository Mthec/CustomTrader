package mod.wurmunlimited.npcs.customtrader.stock;

import com.wurmonline.server.items.InscriptionData;
import com.wurmonline.server.items.Item;

import java.util.Arrays;

public class StockItem {
    public final int templateId;
    public final float ql;
    public final int price;
    public final byte material;
    public final byte rarity;
    public final int weight;
    public final Enchantment[] enchantments;
    public final byte aux;
    public final String inscription;

    public StockItem(int templateId, float ql, int price, byte material, byte rarity, int weight, Enchantment[] enchantments, byte aux, String inscription) {
        this.templateId = templateId;
        this.ql = ql;
        this.price = price;
        this.material = material;
        this.rarity = rarity;
        this.weight = weight;
        this.enchantments = enchantments;
        this.aux = aux;
        this.inscription = inscription;
    }

    public boolean matches(Item item) {
        return item.getTemplateId() == templateId &&
                       item.getQualityLevel() == ql &&
                       item.getMaterial() == material &&
                       item.getRarity() == rarity &&
                       item.getWeightGrams() == weight &&
                       Arrays.equals(Enchantment.parseEnchantments(item), enchantments) &&
                       item.getAuxData() == aux &&
                       (!item.canHaveInscription() || getInscription(item).equals(inscription));
    }

    public static String getInscription(Item item) {
        InscriptionData data = item.getInscription();
        if (data != null) {
            return data.getInscription();
        } else {
            return "";
        }
    }

    @Override
    public int hashCode() {
        int result = 3;
        result = 31 * result + templateId;
        result = 31 * result + Float.floatToIntBits(ql);
        result = 31 * result + price;
        result = 31 * result + (int)material;
        result = 31 * result + (int)rarity;
        result = 31 * result + weight;
        result = 31 * result + Arrays.hashCode(enchantments);
        result = 31 * result + (int)aux;
        result = 31 * result + inscription.hashCode();

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
                   item.weight == weight &&
                   Arrays.equals(item.enchantments, enchantments) &&
                   item.aux == aux &&
                   item.inscription.equals(inscription);
        }

        return super.equals(obj);
    }
}
