package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;


public class TeamDemoteCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("demote")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    //System.out.println("[DEBUG][PROMOTE] Suggestions loaded:");

                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player p)) return builder.buildFuture();

                    UUID uuid = p.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);
                    if (team == null) return builder.buildFuture();

                    TeamMessengerListener.getAllTeamMembers(team).stream()
                        .filter(id -> !id.equals(uuid))
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(Objects::nonNull)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player executor)) return Command.SINGLE_SUCCESS;

                    String name = StringArgumentType.getString(ctx, "player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                    if (target == null || target.getName() == null) {
                        sender.sendMessage(MessagesUtil.get("team.demote.not-found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID execId = executor.getUniqueId();
                    UUID targetId = target.getUniqueId();
                    TeamManager manager = Teams.getInstance().getTeamManager();
                    Team team = manager.getTeamByPlayer(execId);
                    if (team == null) {
                        sender.sendMessage(MessagesUtil.get("team.demote.not-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
                        sender.sendMessage(MessagesUtil.get("team.demote.not-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    TeamRoles execRole = TeamManager.getRole(team, execId);
                    TeamRoles oldRole = TeamManager.getRole(team, targetId);


                    String newRole = switch (oldRole) {
                        case ADMIN -> TeamRoles.MOD.name();
                        case MOD -> TeamRoles.MEMBER.name();
                        default -> null;
                    };

                    if (newRole == null) {
                        sender.sendMessage(MessagesUtil.get("team.demote.invalid-target"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!canDemote(execRole, oldRole)) {
                        sender.sendMessage(MessagesUtil.get("team.demote.no-permission"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!manager.demoteToRole(team.getId(), targetId, newRole)) {
                        sender.sendMessage(MessagesUtil.get("team.demote.failed"));
                        return Command.SINGLE_SUCCESS;
                    }

                    sender.sendMessage(MessagesUtil.get("team.demote.success")
                        .replace("%target%", target.getName())
                        .replace("%oldrole%", oldRole.name())
                        .replace("%newrole%", newRole));

                    Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());



                    TeamMessengerListener.broadcastWithTwo(updatedTeam, execId, targetId, MessagesUtil.get("team.notifications.demoted")
                        .replace("%target%", target.getName())
                        .replace("%executor%", executor.getName())
                        .replace("%oldrole%", oldRole.name())
                        .replace("%newrole%", newRole));

                    manager.logAudit(
                        team.getId(),
                        executor.getUniqueId(),
                        "Demote",
                        executor.getName() + " demoted " + target.getName() + " from " + oldRole.name() + " to " + newRole + "."
                    );


                    return Command.SINGLE_SUCCESS;
                })).build();
    }

    private static boolean canDemote(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MOD) return true;
        return false;
    }
}
