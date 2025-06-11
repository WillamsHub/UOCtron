package edu.uoc.uoctron.utils;

public class Utils {

    public static String snakeCaseToNormal(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String lowerSpaced = input.toLowerCase().replace("_", " ");
        return lowerSpaced.substring(0, 1).toUpperCase() + lowerSpaced.substring(1);
    }

    public static String fromCamelOrPascalToSentence(String input) {
        if (input == null || input.isEmpty()) return input;

        String withSpaces = input.replaceAll("(?<!^)([A-Z])", " $1");
        String lower = withSpaces.toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

}
