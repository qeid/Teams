package discord.qeid.utils;

import org.bukkit.Sound;

public class SoundUtil {

    /**
     * Gets a sound from messages.yml by key.
     * Example: SoundUtil.get("team.notificationsound")
     * Returns null if not found or invalid.
     */
    public static Sound get(String path) {
        String soundString = MessagesUtil.getMessages().getString(path, null);
        if (soundString == null) return null;
        String soundName = soundString.replace('.', '_').toUpperCase();
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            // Invalid sound, just skip
            return null;
        }
    }
}
