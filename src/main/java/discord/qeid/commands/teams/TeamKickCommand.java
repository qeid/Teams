package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.model.TeamRoles;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.DebugUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamKickCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        /*         * This command allows team members to kick other players from their team.
         * It checks permissions based on roles and handles the kick reason.
         * The command can be used by team owners, admins, and mods to manage team membership.
         */

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
            sender.sendMessage(MessagesUtil.get("team.kick.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(MessagesUtil.get("team.kick.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        UUID targetId = target.getUniqueId();
        if (!TeamMessengerListener.getAllTeamMembers(team).contains(targetId)) {
            sender.sendMessage(MessagesUtil.get("team.kick.not-in-team"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        if (targetId.equals(executorId)) {
            sender.sendMessage(MessagesUtil.get("team.kick.self"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        TeamRoles executorRole = teamManager.getRole(team, executorId);
        TeamRoles targetRole = teamManager.getRole(team, targetId);

        if (!canKick(executorRole, targetRole)) {
            sender.sendMessage(MessagesUtil.get("team.kick.no-permission"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }

        boolean kicked = teamManager.kickMember(team.getId(), targetId);
        if (!kicked) {
            sender.sendMessage(MessagesUtil.get("team.kick.failed"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return Command.SINGLE_SUCCESS;
        }


        DebugUtil.sendTeamDebugInfo(executor, team);


        // refresh team after kick
        team = teamManager.getTeamById(team.getId());

        Player targetOnline = Bukkit.getPlayer(targetId);
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(MessagesUtil.get("team.kick.you-were-kicked")
                .replace("%reason%", reason)
                .replace("%executor%", executor.getName()
                .replace("%team%", team.getName())));
            targetOnline.playSound(targetOnline.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0F, 1.5F);
        }

        sender.sendMessage(MessagesUtil.get("team.kick.success")
                .replace("%target%", target.getName())
                .replace("%reason%", reason)
                .replace("%team%", team.getName()));
        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);


        Team updatedTeam = Teams.getInstance().getTeamManager().getTeamById(team.getId());
        TeamMessengerListener.broadcastWithRank(updatedTeam, executorId, MessagesUtil.get("team.notifications.player-kicked")
                .replace("%target%", target.getName())
                .replace("%reason%", reason)
                .replace("%executor%", executor.getName()
                .replace("%team%", team.getName())));



        teamManager.logAudit(
            team.getId(),
            executor.getUniqueId(),
            "Kick",
            executor.getName() + " kicked " + target.getName() + " from the team. Reason: " + reason
        );

        return Command.SINGLE_SUCCESS;
    }

    private static boolean canKick(TeamRoles executor, TeamRoles target) {
        if (executor == TeamRoles.OWNER) return true;
        if (executor == TeamRoles.ADMIN && target == TeamRoles.MEMBER) return true;
        if (executor == TeamRoles.MOD && target == TeamRoles.MEMBER) return true;
        return false;
    }


}
