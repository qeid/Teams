package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import discord.qeid.utils.SoundUtil;
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
                    player.sendMessage(MessagesUtil.get("team.leave.not-in-team"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                if (team.getOwner().equals(playerId)) {
                    player.sendMessage(MessagesUtil.get("team.leave.owner-cannot-leave"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                boolean removed = teamManager.kickMember(team.getId(), playerId);
                if (!removed) {
                    player.sendMessage(MessagesUtil.get("team.leave.failed"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                player.sendMessage(MessagesUtil.get("team.leave.success")
                    .replace("%team%", team.getName()));
                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
                TeamMessengerListener.broadcastExcluding(updatedTeam, playerId,
                    MessagesUtil.get("team.notifications.player-left")
                        .replace("%player%", player.getName()));

                team = teamManager.getTeamById(team.getId());
                DebugUtil.sendTeamDebugInfo(player, team);

                teamManager.logAudit(
                team.getId(),
                player.getUniqueId(),
                "Leave",
                player.getName() + " left the team."
            );


                return Command.SINGLE_SUCCESS;


            }).build();
    }
}
