package discord.qeid.listeners;

import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamMessengerListener {

    public static void broadcast(Team team, String rawMessage) {
        String prefix = ConfigUtil.get("team.prefix").replace("%tag%", team.getTag());
        String formattedMessage = ColorUtils.format(rawMessage);

        for (UUID uuid : getAllOnlineTeamMembers(team)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(prefix + formattedMessage);
            }
        }
    }

    public static void broadcastExcluding(Team team, UUID excludePlayer, String message) {
        String component = ColorUtils.format(message);
        getAllTeamMembers(team).stream()
            .filter(uuid -> !uuid.equals(excludePlayer))
            .map(Bukkit::getPlayer)
            .filter(p -> p != null && p.isOnline())
            .forEach(p -> p.sendMessage(component));
    }


    private static Iterable<UUID> getAllOnlineTeamAdmins(Team team) {
        return team.getAdmins().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Iterable<UUID> getAllOnlineTeamMods(Team team) {
        return team.getMods().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .collect(java.util.stream.Collectors.toSet());
    }

    public static java.util.Set<UUID> getAllTeamMembers(Team team) {
        java.util.Set<UUID> members = new java.util.HashSet<>();
        members.add(team.getOwner());
        members.addAll(team.getAdmins());
        members.addAll(team.getMods());
        members.addAll(team.getMembers());
        return members;
    }

    private static java.util.Set<UUID> getAllOnlineTeamMembersFlat(Team team) {
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (UUID uuid : getAllTeamMembers(team)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                online.add(uuid);
            }
        }
        return online;
    }


    private static java.util.Set<UUID> getAllOnlineTeamMembers(Team team) {
        return getAllOnlineTeamMembersFlat(team);
    }
}
