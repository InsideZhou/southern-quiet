package me.insidezhou.southernquiet.util;

public final class Concat {
    public static String tab(CharSequence... items) {
        return String.join("\t", items);
    }

    public static String comma(CharSequence... items) {
        return String.join(",", items);
    }

    public static String commaWithSpace(CharSequence... items) {
        return String.join(", ", items);
    }
}
