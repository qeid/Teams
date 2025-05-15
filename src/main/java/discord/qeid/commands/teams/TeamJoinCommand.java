package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import discord.qeid.utils.DurationUtil;
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
                        player.sendMessage(ConfigUtil.get("team.join.team-null"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String teamId = targetTeam.getId();

                    //
                    if (teamManager.getTeamByPlayer(playerId) != null) {
                        player.sendMessage(ConfigUtil.get("team.join.already-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (teamManager.isBanned(teamId, playerId)) {
                        var banInfo = teamManager.getBanInfo(teamId, playerId);
                        player.sendMessage(ConfigUtil.get("team.join.banned")
                            .replace("%reason%", banInfo.reason())
                            .replace("%duration%", DurationUtil.formatDurationUntil(banInfo.expiresAt())));
                        return Command.SINGLE_SUCCESS;
                    }

                    //
                    if (!dataManager.hasInvite(playerId, teamId)) {
                        player.sendMessage(ConfigUtil.get("team.join.no-pending-invite"));
                        return Command.SINGLE_SUCCESS;
                    }

                    Team team = teamManager.getTeamById(teamId);
                    if (team == null) {
                        player.sendMessage(ConfigUtil.get("team.join.team-null"));
                        return Command.SINGLE_SUCCESS;
                    }

                    boolean added = teamManager.addMemberToTeam(teamId, playerId);
                    if (!added) {
                        player.sendMessage(ConfigUtil.get("team.join.join-failed"));
                        return Command.SINGLE_SUCCESS;
                    }






                    dataManager.removeInvite(playerId, teamId);
                    player.sendMessage(ConfigUtil.get("team.join.join-success").replace("%team%", team.getName()));
                    Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
                    TeamMessengerListener.broadcast(updatedTeam, ConfigUtil.get("team.notifications.player-joined").replace("%player%", player.getName()));

                    team = teamManager.getTeamById(team.getId());
                    DebugUtil.sendTeamDebugInfo(player, team);

                    return Command.SINGLE_SUCCESS;
                })
            ).build();
    }
}
