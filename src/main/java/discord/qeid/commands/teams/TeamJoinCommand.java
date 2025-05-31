package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.*;
import discord.qeid.listeners.TeamMessengerListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamJoinCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        /*        * This command allows players to join a team they have been invited to.
         * It checks if the player has an invite, is not already in a team, and is not banned.
         * If all conditions are met, the player is added to the team and notified.
         */

        return Commands.literal("join")
            .then(Commands.argument("team invites", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) return builder.buildFuture();

                    UUID playerId = player.getUniqueId();
                    Set<String> teamIds = Teams.getInstance().getPlayerDataManager().getInvites(playerId);


                    TeamManager manager = Teams.getInstance().getTeamManager();
                    teamIds.stream()
                        .map(manager::getTeamById)
                        .filter(team -> team != null)
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
                        return Command.SINGLE_SUCCESS;
                    }

                    String input = StringArgumentType.getString(ctx, "team invites");
                    UUID playerId = player.getUniqueId();

                    Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
                        TeamManager teamManager = Teams.getInstance().getTeamManager();
                        var dataManager = Teams.getInstance().getPlayerDataManager();

                        Team targetTeam = teamManager.getTeamByName(input);
                        if (targetTeam == null) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.null"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        String teamId = targetTeam.getId();

                        if (teamManager.getTeamByPlayer(playerId) != null) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.already-in-team"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        if (teamManager.isBanned(teamId, playerId)) {
                            var banInfo = teamManager.getBanInfo(teamId, playerId);
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.banned")
                                    .replace("%reason%", banInfo.reason())
                                    .replace("%duration%", DurationUtil.formatDurationUntil(banInfo.expiresAt())));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        if (!dataManager.hasInvite(playerId, teamId)) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.no-pending-invite"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        Team team = teamManager.getTeamById(teamId);
                        if (team == null) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.null"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        boolean added = teamManager.addMemberToTeam(teamId, playerId);
                        if (!added) {
                            Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                                player.sendMessage(MessagesUtil.get("team.join.failed"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            });
                            return;
                        }

                        dataManager.removeInvite(playerId, teamId);

                        Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                            player.sendMessage(MessagesUtil.get("team.join.success").replace("%team%", team.getName()));
                            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                            Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
                            TeamMessengerListener.broadcast(updatedTeam, MessagesUtil.get("team.notifications.player-joined").replace("%player%", player.getName()));

                            Teams.getInstance().getTeamManager().logAudit(
                                team.getId(),
                                playerId,
                                "Join",
                                player.getName() + " joined the team."
                            );
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                })
            ).build();
    }
}
