package com.example.bookmark.dish;

public final class TasteProfile {

    private TasteProfile() {
    }

    public record Scores(int spice, int salt, int light) {
    }

    public static int clamp(int value) {
        return Math.max(0, Math.min(5, value));
    }

    public static Scores infer(String name, String tags, String note) {
        String text = String.join(" ",
                name == null ? "" : name,
                tags == null ? "" : tags,
                note == null ? "" : note);

        int spice = 1;
        if (containsAny(text, "变态辣", "特辣", "很辣", "麻辣", "香辣", "辣子", "水煮")) {
            spice = 5;
        } else if (containsAny(text, "中辣", "重辣", "辣")) {
            spice = 4;
        } else if (containsAny(text, "微辣", "少辣")) {
            spice = 2;
        } else if (containsAny(text, "不辣", "免辣")) {
            spice = 0;
        }

        int salt = 2;
        if (containsAny(text, "重口", "很咸", "咸香", "下饭")) {
            salt = 5;
        } else if (containsAny(text, "偏咸", "咸")) {
            salt = 4;
        } else if (containsAny(text, "少盐", "清淡")) {
            salt = 1;
        }

        int light = 2;
        if (containsAny(text, "清淡", "少油", "蒸", "白灼", "养生")) {
            light = 5;
        } else if (containsAny(text, "油腻", "干锅", "红烧", "炸")) {
            light = 1;
        }

        return new Scores(spice, salt, light);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
