package com.wurmonline.server.questions;

class Details {
    final float ql;
    final int materialIndex;
    final byte rarity;
    final int price;
    final int weight;
    final byte aux;
    final String inscription;

    // Note - Repurposed in AdvancedCurrencyQuestion, where aux stands in for material.  Not good, past Mthec.
    Details(float ql, int materialIndex, byte rarity, int price, int weight, byte aux, String inscription) {
        this.ql = ql;
        this.materialIndex = materialIndex;
        this.rarity = rarity;
        this.price = price;
        this.weight = weight;
        this.aux = aux;
        this.inscription = inscription;
    }

    static Details _default(int materialIndex, int weight) {
        return new Details(20f, materialIndex, (byte)0, 1, weight, (byte)0, "");
    }
}
