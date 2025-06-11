package edu.uoc.uoctron.utils;

public class Utils {

    public static String toCapitalizedSentence(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String lower = input.toLowerCase();
        String spaced = lower.replace("_", " ");

        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

}
