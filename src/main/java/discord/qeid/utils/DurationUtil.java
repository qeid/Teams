package discord.qeid.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*(seconds?|s|minutes?|m|hours?|h|days?|d|weeks?|w)",
        Pattern.CASE_INSENSITIVE
    );

    public static long parseDurationSeconds(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalSeconds = 0;

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit) {
                case "s":
                case "second":
                case "seconds":
                    totalSeconds += value;
                    break;
                case "m":
                case "minute":
                case "minutes":
                    totalSeconds += value * 60;
                    break;
                case "h":
                case "hour":
                case "hours":
                    totalSeconds += value * 60 * 60;
                    break;
                case "d":
                case "day":
                case "days":
                    totalSeconds += value * 86400;
                    break;
                case "w":
                case "week":
                case "weeks":
                    totalSeconds += value * 604800;
                    break;
            }
        }

        return totalSeconds;
    }

    public static String stripDuration(String input) {
        return DURATION_PATTERN.matcher(input).replaceAll("").trim();
    }

    public static String formatDurationUntil(long unixTimestamp) {
        if (unixTimestamp <= 0) return "Permanent";

        long now = System.currentTimeMillis() / 1000;
        long secondsLeft = unixTimestamp - now;

        if (secondsLeft <= 0) return "Expired";

        return formatFullDuration(secondsLeft);
    }

    public static String formatFullDuration(long seconds) {
        if (seconds <= 0) return "Expired";

        long weeks = seconds / 604800; seconds %= 604800;
        long days = seconds / 86400;   seconds %= 86400;
        long hours = seconds / 3600;   seconds %= 3600;
        long minutes = seconds / 60;   seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append(" week").append(weeks > 1 ? "s" : "").append(", ");
        if (days > 0) sb.append(days).append(" day").append(days > 1 ? "s" : "").append(", ");
        if (hours > 0) sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(", ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(", ");
        if (seconds > 0) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "").append(", ");

        // Remove last comma
        String output = sb.toString().trim();
        return output.endsWith(",") ? output.substring(0, output.length() - 1) : output;
    }
}

