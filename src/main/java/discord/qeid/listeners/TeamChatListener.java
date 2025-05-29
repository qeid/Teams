package discord.qeid.listeners;

import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

import static discord.qeid.utils.ColorUtils.coloredRank;
import static discord.qeid.utils.ColorUtils.formatLegacy;

public class TeamChatListener implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        var dataManager = Teams.getInstance().getPlayerDataManager();
        if (!dataManager.isTeamChatToggled(uuid)) return;

        var teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(uuid);
        if (team == null) return;

        TeamRoles role = discord.qeid.database.TeamManager.getRole(team, uuid);
        String rank = coloredRank(role, true);

        String format = MessagesUtil.get("team.chat.format")
            .replace("%tag%", team.getTag())
            .replace("%player%", player.getName())
            .replace("%message%", event.getMessage())
            .replace("%rank%", rank);


        var members = discord.qeid.listeners.TeamMessengerListener.getAllTeamMembers(team);


        event.setCancelled(true);


        org.bukkit.Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
            for (UUID member : members) {
                Player p = player.getServer().getPlayer(member);
                if (p != null && p.isOnline()) {
                    p.sendMessage(formatLegacy(format));
                    p.playSound(p.getLocation(), SoundUtil.get("team.sounds.notification"), 1.0F, 1.0F);
                }
            }
        });
    }

}
