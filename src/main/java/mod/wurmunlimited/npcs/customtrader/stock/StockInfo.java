package mod.wurmunlimited.npcs.customtrader.stock;

public class StockInfo {
    public final StockItem item;
    public final int maxNum;
    public final int restockRate;
    public final int restockInterval;

    public StockInfo(StockItem item, int maxNum, int restockRate, int restockInterval) {
        this.item = item;
        this.maxNum = maxNum;
        this.restockRate = restockRate;
        this.restockInterval = restockInterval;
    }
}
