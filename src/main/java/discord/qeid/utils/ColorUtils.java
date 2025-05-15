package discord.qeid.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    // Pattern for matching hex color codes like &#abcdef
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String format(String message) {
        if (message == null) return "";

        // Convert hex colors first
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");

            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }

            matcher.appendReplacement(buffer, replacement.toString());
        }

        matcher.appendTail(buffer);

        // Now translate legacy color codes (&a, &l, etc.)
        return buffer.toString().replaceAll("&([0-9a-fk-or])", "§$1");
    }
}
