package com.wurmonline.server.questions;

class Details {
    final float ql;
    final int materialIndex;
    final byte rarity;
    final int price;
    final int weight;

    Details(float ql, int materialIndex, byte rarity, int price, int weight) {
        this.ql = ql;
        this.materialIndex = materialIndex;
        this.rarity = rarity;
        this.price = price;
        this.weight = weight;
    }

    static Details _default(int materialIndex, int weight) {
        return new Details(20f, materialIndex, (byte)0, 1, weight);
    }
}
