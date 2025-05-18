package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.ConfigUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamKickCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("kick")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> { CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        //System.out.println("[debug - kick] not a player");
                        return builder.buildFuture();
                    }

                    UUID uuid = player.getUniqueId();
                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(uuid);

                    if (team == null) {
                        //System.out.println("[debug - kick] no team found for player: " + player.getName());
                        return builder.buildFuture();
                    }

                    //System.out.println("[debug - kick] suggesting names ...");

                    Set<UUID> allMembers = TeamMessengerListener.getAllTeamMembers(team);
                    allMembers.stream()
                        .filter(id -> !id.equals(uuid))
                        .map(Bukkit::getOfflinePlayer)
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        //.peek(name -> System.out.println("[DEBUG][KICK] Suggesting: " + name))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })

                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> execute(ctx.getSource(),
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))
                )
                .executes(ctx -> execute(ctx.getSource(),
                    StringArgumentType.getString(ctx, "player"),
                    "No reason specified"))
            ).build();
    }

    private static int execute(CommandSourceStack source, String targetName, String reason) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can run this."));
            return Command.SINGLE_SUCCESS;
        }

        UUID executorId = executor.getUniqueId();
        var teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByPlayer(executorId);

        if (team == null) {
            sender.sendMessage(ConfigUtil.get("team.kick.not-in-team"));
            return Command.SINGLE_SUCCESS;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(ConfigUtil.get("team.kick.not-in-team"));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetId = target.getUniqueId();
        if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
            sender.sendMessage(ConfigUtil.get("team.kick.not-in-team"));
            return Command.SINGLE_SUCCESS;
        }

        if (targetId.equals(executorId)) {
            sender.sendMessage(ConfigUtil.get("team.kick.self"));
            return Command.SINGLE_SUCCESS;
        }

        TeamRoles executorRole = teamManager.getRole(team, executorId);
        TeamRoles targetRole = teamManager.getRole(team, targetId);

        if (!canKick(executorRole, targetRole)) {
            sender.sendMessage(ConfigUtil.get("team.kick.no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        boolean kicked = teamManager.kickMember(team.getId(), targetId);
        if (!kicked) {
            sender.sendMessage(ConfigUtil.get("team.kick.failed"));
            return Command.SINGLE_SUCCESS;
        }


        DebugUtil.sendTeamDebugInfo(executor, team);


        // refresh team after kick
        team = teamManager.getTeamById(team.getId());

        Player targetOnline = Bukkit.getPlayer(targetId);
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(ConfigUtil.get("team.kick.you-were-kicked")
                .replace("%reason%", reason)
                .replace("%executor%", executor.getName()
                .replace("%team%", team.getName())));
        }

        sender.sendMessage(ConfigUtil.get("team.kick.success")
                .replace("%target%", target.getName())
                .replace("%reason%", reason)
                .replace("%team%", team.getName()));


        Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
        TeamMessengerListener.broadcastWithRank(updatedTeam, executorId, ConfigUtil.get("team.notifications.player-kicked")
                .replace("%target%", target.getName())
                .replace("%reason%", reason)
                .replace("%executor%", executor.getName()
                .replace("%team%", team.getName())));

        // TODO: Audit logging here
        return Command.SINGLE_SUCCESS;
    }

    private static boolean canKick(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MEMBER) return true;
        if (executor == TeamRoles.MOD && target == TeamRoles.MEMBER) return true;
        return false;
    }


}
