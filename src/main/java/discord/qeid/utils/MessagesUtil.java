package discord.qeid.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MessagesUtil {
    private static FileConfiguration messages;

    public static void loadMessages(JavaPlugin plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static String get(String path) {
        return ColorUtils.formatLegacy(messages.getString(path, "&cMessage not found: " + path));
    }
    public static FileConfiguration getMessages() {
        return messages;
    }
}
