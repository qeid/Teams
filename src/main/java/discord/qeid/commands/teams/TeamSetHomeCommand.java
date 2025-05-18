package discord.qeid.commands.teams;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ConfigUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.Command;

public class TeamSetHomeCommand {
    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("sethome")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(ConfigUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }
                TeamRoles role = TeamManager.getRole(team, player.getUniqueId());
                if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
                    player.sendMessage(ConfigUtil.get("team.sethome.no-permission"));
                    return Command.SINGLE_SUCCESS;
                }
                boolean success = teamManager.setHome(team.getId(), player.getLocation());
                if (success) {
                    var loc = player.getLocation();
                    String msg = ConfigUtil.get("team.sethome.success")
                        .replace("%x%", String.format("%.2f", loc.getX()))
                        .replace("%y%", String.format("%.2f", loc.getY()))
                        .replace("%z%", String.format("%.2f", loc.getZ()));
                    player.sendMessage(msg);

                    // Broadcast to team members (excluding the player if you want)
                    String broadcastMsg = ConfigUtil.get("team.notifications.home-set")
                        .replace("%executor%", player.getName())
                        .replace("%x%", String.format("%.2f", loc.getX()))
                        .replace("%y%", String.format("%.2f", loc.getY()))
                        .replace("%z%", String.format("%.2f", loc.getZ()));
                    TeamMessengerListener.broadcastWithRank(team, player.getUniqueId(), broadcastMsg);
                } else {
                    player.sendMessage(ConfigUtil.get("team.sethome.failed"));
                }
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}

