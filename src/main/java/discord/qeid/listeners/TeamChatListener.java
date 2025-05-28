package discord.qeid.listeners;

import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

import static discord.qeid.utils.ColorUtils.coloredRank;

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
    
        for (UUID member : discord.qeid.listeners.TeamMessengerListener.getAllTeamMembers(team)) {
            Player p = player.getServer().getPlayer(member);
            if (p != null && p.isOnline()) {
                p.sendMessage(LEGACY.deserialize(format));
            }
        }
        event.setCancelled(true);
    }

}
