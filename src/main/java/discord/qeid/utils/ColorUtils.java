package discord.qeid.utils;

import discord.qeid.model.TeamRoles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    public static Component format(String message) {
        if (message == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }



    public static String formatLegacy(String message) {
        if (message == null) return "";

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
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
        return buffer.toString().replaceAll("&([0-9a-fk-or])", "§$1");
    }


    public static String applyTagCase(String tag, String caseType) {
        if (tag == null) return null;
        switch (caseType.toLowerCase()) {
            case "upper": return tag.toUpperCase();
            case "lower": return tag.toLowerCase();
            case "proper":
                if (tag.length() == 0) return tag;
                return tag.substring(0, 1).toUpperCase() + tag.substring(1).toLowerCase();
            default: return tag;
        }
    }

    public static String coloredRank(TeamRoles role, boolean pretty) {
        String color;
        String rank;
        switch (role) {
            case OWNER:  color = "&4"; rank = "OWNER"; break;
            case ADMIN:  color = "&c"; rank = "ADMIN"; break;
            case MOD:    color = "&e"; rank = "MOD"; break;
            case MEMBER: color = "&7"; rank = "MEMBER"; break;
            default:     color = "&8"; rank = "NONE"; break;
        }
        if (pretty) {
            rank = rank.charAt(0) + rank.substring(1).toLowerCase(); // "Admin"
        }
        return color + rank;
    }


}