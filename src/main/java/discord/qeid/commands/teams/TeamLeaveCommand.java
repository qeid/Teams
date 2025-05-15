package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamLeaveCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("leave")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("&cOnly players can use this command.");
                    return Command.SINGLE_SUCCESS;
                }

                UUID playerId = player.getUniqueId();
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(playerId);

                if (team == null) {
                    player.sendMessage(ConfigUtil.get("team.leave.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }

                if (team.getOwner().equals(playerId)) {
                    player.sendMessage(ConfigUtil.get("team.leave.owner-cannot-leave"));
                    return Command.SINGLE_SUCCESS;
                }

                boolean removed = teamManager.kickMember(team.getId(), playerId);
                if (!removed) {
                    player.sendMessage(ConfigUtil.get("team.leave.failed"));
                    return Command.SINGLE_SUCCESS;
                }

                player.sendMessage(ConfigUtil.get("team.leave.success")
                    .replace("%team%", team.getName()));

                Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
                TeamMessengerListener.broadcastExcluding(updatedTeam, playerId,
                    ConfigUtil.get("team.notifications.player-left")
                        .replace("%player%", player.getName()));

                team = teamManager.getTeamById(team.getId());
                DebugUtil.sendTeamDebugInfo(player, team);

                return Command.SINGLE_SUCCESS;


            }).build();
    }
}
