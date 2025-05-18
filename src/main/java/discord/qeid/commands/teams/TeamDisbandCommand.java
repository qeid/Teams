package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamDisbandCommand {

    private static final Map<UUID, Long> pendingDisbands = new HashMap<>();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("disband")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("&cOnly players can use this command.");
                    return Command.SINGLE_SUCCESS;
                }

                UUID uuid = player.getUniqueId();
                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(uuid);

                if (team == null) {
                    player.sendMessage(MessagesUtil.get("team.disband.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }

                if (!team.getOwner().equals(uuid)) {
                    player.sendMessage(MessagesUtil.get("team.disband.not-owner"));
                    return Command.SINGLE_SUCCESS;
                }

                long now = System.currentTimeMillis();

                if (pendingDisbands.containsKey(uuid)) {
                    long started = pendingDisbands.get(uuid);
                    if (now - started <= 10_000) {
                        pendingDisbands.remove(uuid);

                        boolean deleted = teamManager.disbandTeam(team.getId());
                        if (!deleted) {
                            player.sendMessage(MessagesUtil.get("team.disband.failed"));
                            return Command.SINGLE_SUCCESS;
                        }

                        player.sendMessage(MessagesUtil.get("team.disband.success")
                            .replace("%team%", team.getName()));

                        Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());

                        TeamMessengerListener.broadcastExcluding(updatedTeam, uuid,
                            MessagesUtil.get("team.notifications.owner-disbanded")
                                .replace("%player%", player.getName())
                                .replace("%team%", team.getName()));
                        return Command.SINGLE_SUCCESS;
                    } else {
                        pendingDisbands.remove(uuid);
                        player.sendMessage(MessagesUtil.get("team.disband.timeout"));
                        return Command.SINGLE_SUCCESS;
                    }
                }

                pendingDisbands.put(uuid, now);
                player.sendMessage(MessagesUtil.get("team.disband.confirm"));
                Bukkit.getScheduler().runTaskLater(Teams.getInstance(), () -> {
                    if (pendingDisbands.containsKey(uuid)) {
                        pendingDisbands.remove(uuid);
                        if (Teams.getInstance().getTeamManager().getTeamByPlayer(uuid) != null) {
                            player.sendMessage(MessagesUtil.get("team.disband.timeout"));
                        }
                    }
                }, 200L); // 10 seconds

                team = teamManager.getTeamById(team.getId());
                DebugUtil.sendTeamDebugInfo(player, team);
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
