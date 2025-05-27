package discord.qeid.listeners;

import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

import static discord.qeid.database.TeamManager.getRole;
import static discord.qeid.utils.ColorUtils.coloredRank;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class TeamMessengerListener {

    public static void broadcast(Team team, String rawMessage) {
        String prefix = MessagesUtil.get("team.prefix").replace("%tag%", team.getTag());
        String formattedMessage = ColorUtils.formatLegacy(rawMessage);

        for (UUID uuid : getAllOnlineTeamMembers(team)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(prefix + formattedMessage);
            }
        }
    }

    public static void broadcastExcluding(Team team, UUID excludePlayer, String message) {
        String component = ColorUtils.formatLegacy(message);
        getAllTeamMembers(team).stream()
            .filter(uuid -> !uuid.equals(excludePlayer))
            .map(Bukkit::getPlayer)
            .filter(p -> p != null && p.isOnline())
            .forEach(p -> p.sendMessage(component));
    }

    public static void broadcastWithRank(Team team, UUID actor, String rawMessage) {
        String prefix = MessagesUtil.get("team.prefix").replace("%tag%", team.getTag());
        TeamRoles role = getRole(team, actor);

        String coloredPretty = coloredRank(role, true);   // e.g. &cAdmin
        String coloredCaps   = coloredRank(role, false);  // e.g. &cADMIN

        String formattedMessage = ColorUtils.formatLegacy(
            rawMessage
                .replace("%rank%", coloredPretty)
                .replace("%rank-caps%", coloredCaps)
        );

        for (UUID uuid : getAllOnlineTeamMembers(team)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(prefix + formattedMessage);
            }
        }
    }

    public static void broadcastWithTwo(Team team, UUID actor, UUID target, String rawMessage) {
            String prefix = MessagesUtil.get("team.prefix").replace("%tag%", team.getTag());

            TeamRoles actorRole = getRole(team, actor);
            TeamRoles targetRole = getRole(team, target);

            String actorPretty = coloredRank(actorRole, true);
            String actorCaps   = coloredRank(actorRole, false);
            String targetPretty = coloredRank(targetRole, true);
            String targetCaps   = coloredRank(targetRole, false);

            String formattedMessage = ColorUtils.formatLegacy(
                rawMessage
                    .replace("%rank%", actorPretty)
                    .replace("%rank-caps%", actorCaps)
                    .replace("%target-rank%", targetPretty)
                    .replace("%target-rank-caps%", targetCaps)
            );

            for (UUID uuid : getAllOnlineTeamMembers(team)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(prefix + formattedMessage);
                }
            }
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
