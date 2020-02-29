package com.wurmonline.server.questions;

class Restocking {
    final int maxStock;
    final int restockRate;
    final int restockInterval;

    Restocking(int maxStock, int restockRate, int restockInterval) {
        this.maxStock = maxStock;
        this.restockRate = restockRate;
        this.restockInterval = restockInterval;
    }

    static Restocking _default() {
        return new Restocking(1, 0, 24);
    }
}
