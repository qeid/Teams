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

public class TeamDelHomeCommand {
    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("delhome")
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
                    player.sendMessage(ConfigUtil.get("team.delhome.no-permission"));
                    return Command.SINGLE_SUCCESS;
                }
                boolean success = teamManager.delHome(team.getId());
                if (success) {
                    player.sendMessage(ConfigUtil.get("team.delhome.success"));

                    String broadcastMsg = ConfigUtil.get("team.notifications-home-deleted")
                        .replace("%player%", player.getName());
                    TeamMessengerListener.broadcast(team, broadcastMsg);
                } else {
                    player.sendMessage(ConfigUtil.get("team.delhome.failed"));
                }
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
