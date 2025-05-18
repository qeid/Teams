package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamDelHomeCommand {
    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("delhome")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessagesUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }
                TeamRoles role = teamManager.getRole(team, player.getUniqueId());
                if (role != TeamRoles.ADMIN && role != TeamRoles.OWNER) {
                    player.sendMessage(MessagesUtil.get("team.delhome.no-permission"));
                    return Command.SINGLE_SUCCESS;
                }

                Location home = teamManager.getHome(team.getId());
                if (home == null) {
                    player.sendMessage(MessagesUtil.get("team.delhome.not-set"));
                    return Command.SINGLE_SUCCESS;
                }
                boolean success = teamManager.delHome(team.getId());
                if (success) {
                    player.sendMessage(MessagesUtil.get("team.delhome.success"));
                    String broadcastMsg = MessagesUtil.get("team.delhome.broadcast")
                        .replace("%executor%", player.getName());
                    TeamMessengerListener.broadcastWithRank(team, player.getUniqueId(), broadcastMsg);
                } else {
                    player.sendMessage(MessagesUtil.get("team.delhome.failed"));
                }
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
