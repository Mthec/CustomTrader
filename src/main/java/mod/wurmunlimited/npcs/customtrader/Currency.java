package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.shared.util.MaterialUtilities;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class Currency {
    public enum MatchStatus {
        MATCHES,
        WRONG_TEMPLATE,
        QL_DOES_NOT_MATCH_EXACT,
        QL_TOO_LOW,
        WRONG_MATERIAL,
        WRONG_RARITY,
        DOES_NOT_WEIGH_ENOUGH
    }

    private static final DecimalFormat df = new DecimalFormat("0");
    public final int templateId;
    private final ItemTemplate template;
    public final float minQL;
    public final float exactQL;
    public final byte material;
    public final byte rarity;
    public final boolean onlyFullWeight;

    public Currency(@NotNull ItemTemplate template) {
        this(template, -1f, -1f, (byte)-1, (byte)-1, true);
    }

    public Currency(@NotNull ItemTemplate template, float minQL, float exactQL, byte material, byte rarity, boolean onlyFullWeight) {
        this.templateId = template.getTemplateId();
        this.template = template;
        this.minQL = minQL;
        this.exactQL = exactQL;
        this.material = material;
        this.rarity = rarity;
        this.onlyFullWeight = onlyFullWeight;
    }

    public Currency(int templateId, float minQL, float exactQL, byte material, byte rarity, boolean onlyFullWeight) throws NoSuchTemplateException {
        this.templateId = templateId;
        template = ItemTemplateFactory.getInstance().getTemplate(templateId);
        this.minQL = minQL;
        this.exactQL = exactQL;
        this.material = material;
        this.rarity = rarity;
        this.onlyFullWeight = onlyFullWeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Currency) {
            Currency other = ((Currency)obj);
            return other.templateId == templateId && other.minQL == minQL && other.exactQL == exactQL &&
                   other.material == material && other.rarity == rarity && other.onlyFullWeight == onlyFullWeight;
        }

        return false;
    }

    @Override
    public String toString() {
        return "id:" + templateId + ", " + "name:" + template.getName() + ", " + "minimum_ql:" + minQL + ", " +
                       "exact_ql:" + exactQL + ", " + "material:" + material + ", " + "rarity:" + rarity + ", " +
                       "full_weight:" + onlyFullWeight;
    }

    public ItemTemplate getTemplate() {
        return template;
    }

    public MatchStatus matches(Item item) {
        if (item.getTemplateId() != templateId) {
            return MatchStatus.WRONG_TEMPLATE;
        } else if (exactQL > 0 && item.getQualityLevel() != exactQL) {
            return MatchStatus.QL_DOES_NOT_MATCH_EXACT;
        } else if (item.getQualityLevel() < minQL) {
            return MatchStatus.QL_TOO_LOW;
        } else if (material > 0 && item.getMaterial() != material) {
            return MatchStatus.WRONG_MATERIAL;
        } else if (rarity >= 0 && item.getRarity() != rarity) {
            return MatchStatus.WRONG_RARITY;
        } else if (onlyFullWeight && item.getWeightGrams() != template.getWeightGrams()) {
            return MatchStatus.DOES_NOT_WEIGH_ENOUGH;
        }

        return MatchStatus.MATCHES;
    }

    public String getNameFor(int amount) {
        return (amount == 1 ? getName() : getPlural());
    }

    private StringBuilder getPrefix() {
        StringBuilder sb = new StringBuilder(MaterialUtilities.getRarityString(rarity));

        if (sb.length() == 0 && rarity != -1) {
            sb.append("normal ");
        }

        if (material > 0) {
            sb.append(MaterialUtilities.getMaterialString(material)).append(" ");
        }

        return sb;
    }

    private StringBuilder addSuffix(StringBuilder sb) {
        if (exactQL > 0) {
            sb.append(" of exactly ").append(df.format(exactQL)).append("ql");
        } else if (minQL > 0) {
            sb.append(" of at least ").append(df.format(minQL)).append("ql");
        }

        return sb;
    }
    
    public String getName() {
        StringBuilder sb = getPrefix();
        sb.append(template.getName());
        return addSuffix(sb).toString();
    }

    public String getPlural() {
        StringBuilder sb = getPrefix();
        sb.append(template.getPlural());
        return addSuffix(sb).toString();
    }
}
