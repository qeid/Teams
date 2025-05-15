package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.listeners.TeamMessengerListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamInviteCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("invite")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        builder.suggest(p.getName());
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player inviter)) {
                        sender.sendMessage(ConfigUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String targetName = StringArgumentType.getString(ctx, "player");
                    Player target = Bukkit.getPlayerExact(targetName);

                    if (target == null || !target.isOnline()) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID targetUUID = target.getUniqueId();
                    UUID inviterUUID = inviter.getUniqueId();

                    var teamManager = Teams.getInstance().getTeamManager();
                    var playerData = Teams.getInstance().getPlayerDataManager();

                    Team team = teamManager.getTeamByPlayer(inviterUUID);
                    if (team == null) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.no_team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (teamManager.isBanned(team.getId(), targetUUID)) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.banned"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!inviterUUID.equals(team.getOwner())
                        && !team.getAdmins().contains(inviterUUID)
                        && !team.getMods().contains(inviterUUID)) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.no_permission"));
                        return Command.SINGLE_SUCCESS;
                    }

                    Team targetTeam = teamManager.getTeamByPlayer(targetUUID);
                    if (targetTeam != null) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.already_in_team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (playerData.hasInvite(targetUUID, team.getId())) {
                        inviter.sendMessage(ConfigUtil.get("team.invite.already_invited"));
                        return Command.SINGLE_SUCCESS;
                    }

                    playerData.addInvite(targetUUID, team.getId());

                    Bukkit.getScheduler().runTaskLater(Teams.getInstance(), () -> {
                        if (teamManager.getTeamByPlayer(targetUUID) != null) return;
                        if (!playerData.hasInvite(targetUUID, team.getId())) return;

                        playerData.removeInvite(targetUUID, team.getId());
                        target.sendMessage(ConfigUtil.get("team.invite.expired-invitee").replace("%team%", team.getName()));
                        TeamMessengerListener.broadcast(team,
                            ConfigUtil.get("team.invite.expired-team").replace("%target%", target.getName()));
                    }, 20L * 60 * 5); // 5 minutes

                    inviter.sendMessage(ConfigUtil.get("team.invite.sent").replace("%target%", target.getName()));
                    target.sendMessage(ConfigUtil.get("team.invite.received")
                        .replace("%team%", team.getName())
                        .replace("%sender%", inviter.getName()));

                    TeamMessengerListener.broadcast(team, ConfigUtil.get("team.notifications.invite-sent")
                        .replace("%sender%", inviter.getName())
                        .replace("%target%", target.getName()));

                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
