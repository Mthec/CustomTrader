package com.wurmonline.server.questions;

import java.math.BigDecimal;

public class WeightHelper {
    private static final BigDecimal mod = new BigDecimal(1000);

    public static String toString(int weight) {
        BigDecimal bd = new BigDecimal(weight).setScale(3, BigDecimal.ROUND_UNNECESSARY);
        bd = bd.divide(mod, BigDecimal.ROUND_UNNECESSARY);
        return bd.stripTrailingZeros().toPlainString();
    }

    public static int toInt(String str) {
        return new BigDecimal(str).multiply(mod).intValue();
    }
}