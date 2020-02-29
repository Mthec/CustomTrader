package com.wurmonline.server.questions;

class Details {
    final float ql;
    final int materialIndex;
    final byte rarity;
    final int price;

    Details(float ql, int materialIndex, byte rarity, int price) {
        this.ql = ql;
        this.materialIndex = materialIndex;
        this.rarity = rarity;
        this.price = price;
    }

    static Details _default(int materialIndex) {
        return new Details(20f, materialIndex, (byte)0, 1);
    }
}
