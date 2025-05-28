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


import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamJoinCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("join")
            .then(Commands.argument("team invites", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) return builder.buildFuture();

                    UUID playerId = player.getUniqueId();
                    Set<String> teamIds = Teams.getInstance().getPlayerDataManager().getInvites(playerId);

                    // Map team IDs to their names
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

                    String input = StringArgumentType.getString(ctx, "team invites").trim();
                    UUID playerId = player.getUniqueId();

                    var teamManager = Teams.getInstance().getTeamManager();
                    var dataManager = Teams.getInstance().getPlayerDataManager();

                    //
                    Team targetTeam = teamManager.getTeamByName(input);
                    if (targetTeam == null) {
                        player.sendMessage(MessagesUtil.get("team.join.null"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    String teamId = targetTeam.getId();

                    //
                    if (teamManager.getTeamByPlayer(playerId) != null) {
                        player.sendMessage(MessagesUtil.get("team.join.already-in-team"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (teamManager.isBanned(teamId, playerId)) {
                        var banInfo = teamManager.getBanInfo(teamId, playerId);
                        player.sendMessage(MessagesUtil.get("team.join.banned")
                            .replace("%reason%", banInfo.reason())
                            .replace("%duration%", DurationUtil.formatDurationUntil(banInfo.expiresAt())));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    //
                    if (!dataManager.hasInvite(playerId, teamId)) {
                        player.sendMessage(MessagesUtil.get("team.join.no-pending-invite"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    Team team = teamManager.getTeamById(teamId);
                    if (team == null) {
                        player.sendMessage(MessagesUtil.get("team.join.null"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    boolean added = teamManager.addMemberToTeam(teamId, playerId);
                    if (!added) {
                        player.sendMessage(MessagesUtil.get("team.join.failed"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }






                    dataManager.removeInvite(playerId, teamId);
                    player.sendMessage(MessagesUtil.get("team.join.success").replace("%team%", team.getName()));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);
                    Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
                    TeamMessengerListener.broadcast(updatedTeam, MessagesUtil.get("team.notifications.player-joined").replace("%player%", player.getName()));

                    team = teamManager.getTeamById(team.getId());
                    DebugUtil.sendTeamDebugInfo(player, team);

                    teamManager.logAudit(
                    team.getId(),
                    playerId,
                    "Join",
                    player.getName() + " joined the team."
                );

                    return Command.SINGLE_SUCCESS;
                })
            ).build();
    }
}
