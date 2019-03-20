package com.dimlix.tgcontest.chart;

import java.text.DecimalFormat;
import java.util.Locale;

public class Utils {

    private static char[] c = new char[]{'k', 'm', 'b', 't'};
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###,###");
    public static final String TWO_DIGIT_FORMAT_PATTERN = "%.2f";

    public static String coolFormat(long n) {
        if (n < 1000) return String.valueOf(n);
        int charPos = -1;
        double value = n;
        while (charPos < 3 && (value >= 1000.0 || value <= -
                1000.0)) {
            value /= 1000;
            charPos++;
        }
        if (charPos == -1) {
            return String.format(Locale.US, TWO_DIGIT_FORMAT_PATTERN, value);
        } else {
            return String.format(Locale.US, TWO_DIGIT_FORMAT_PATTERN, value) + c[charPos];
        }
    }

    public static String prettyFormat(long n) {
        return DECIMAL_FORMAT.format(n);
    }
}

