package com.wurmonline.server.questions;

class Details {
    final float ql;
    final int materialIndex;
    final byte rarity;
    final int price;
    final int weight;
    final byte aux;

    Details(float ql, int materialIndex, byte rarity, int price, int weight, byte aux) {
        this.ql = ql;
        this.materialIndex = materialIndex;
        this.rarity = rarity;
        this.price = price;
        this.weight = weight;
        this.aux = aux;
    }

    static Details _default(int materialIndex, int weight) {
        return new Details(20f, materialIndex, (byte)0, 1, weight, (byte)0);
    }
}
