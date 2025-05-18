package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.listeners.TeamMessengerListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;


public class TeamPromoteCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("promote")
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
                        sender.sendMessage(ConfigUtil.get("team.promote.not-found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    UUID execId = executor.getUniqueId();
                    UUID targetId = target.getUniqueId();
                    TeamManager manager = Teams.getInstance().getTeamManager();
                    Team team = manager.getTeamByPlayer(execId);
                    if (team == null) {
                        sender.sendMessage(ConfigUtil.get("team.promote.not-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
                        sender.sendMessage(ConfigUtil.get("team.promote.not-in-team"));
                        return Command.SINGLE_SUCCESS;
                    }

                    TeamRoles execRole = manager.getRole(team, execId);
                    TeamRoles oldRole = manager.getRole(team, targetId);

                    // chain
                    String newRole = switch (oldRole) {
                        case MEMBER -> "Mod";
                        case MOD -> "Admin";
                        default -> null;
                    };

                    if (newRole == null) {
                        sender.sendMessage(ConfigUtil.get("team.promote.invalid-target"));
                        return Command.SINGLE_SUCCESS;
                    }

                    // permission checks
                    if (!canPromote(execRole, oldRole)) {
                        sender.sendMessage(ConfigUtil.get("team.promote.no-permission"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!manager.promoteToRole(team.getId(), targetId, newRole)) {
                        sender.sendMessage(ConfigUtil.get("team.promote.failed"));
                        return Command.SINGLE_SUCCESS;
                    }

                    team = manager.getTeamById(team.getId());


                    sender.sendMessage(ConfigUtil.get("team.promote.success")
                        .replace("%target%", target.getName())
                        .replace("%oldrole%", oldRole.name())
                        .replace("%newrole%", newRole));

                    Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());

                    TeamMessengerListener.broadcast(updatedTeam, ConfigUtil.get("team.notifications.promoted")
                        .replace("%target%", target.getName())
                        .replace("%executor%", executor.getName())
                        .replace("%oldrole%", oldRole.name())
                        .replace("%newrole%", newRole));


                    System.out.println("Team Members after update:");
                    System.out.println("Owner: " + team.getOwner());
                    System.out.println("Admins: " + team.getAdmins());
                    System.out.println("Mods: " + team.getMods());
                    System.out.println("Members: " + team.getMembers());
                    return Command.SINGLE_SUCCESS;
                })).build();
    }

    private static boolean canPromote(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MOD) return true;
        return false;
    }
}
