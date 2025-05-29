package discord.qeid.commands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamAdminForceJoinCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("join")
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    Teams.getInstance().getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String teamName = StringArgumentType.getString(ctx, "team");
                        String playerName = StringArgumentType.getString(ctx, "player");
                        return execute(ctx.getSource().getSender(), teamName, playerName);
                    })
                )
                .executes(ctx -> {
                    String teamName = StringArgumentType.getString(ctx, "team");
                    CommandSender sender = ctx.getSource().getSender();
                    String playerName = sender instanceof Player ? sender.getName() : null;
                    return execute(sender, teamName, playerName);
                })
            ).build();
    }

    private static int execute(CommandSender sender, String teamName, String playerName) {
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(MessagesUtil.get("admin.forcejoin.team-not-found"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        OfflinePlayer target;
        if (playerName == null) {
            sender.sendMessage(MessagesUtil.get("admin.forcejoin.player-not-found"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }
        target = Bukkit.getOfflinePlayer(playerName);

        if (target == null || target.getName() == null) {
            sender.sendMessage(MessagesUtil.get("admin.forcejoin.player-not-found"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        UUID targetId = target.getUniqueId();

        if (teamManager.getTeamByPlayer(targetId) != null) {
            sender.sendMessage(MessagesUtil.get("admin.forcejoin.already-in-team")
                .replace("%player%", target.getName()));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        boolean added = teamManager.addMemberToTeam(team.getId(), targetId);
        if (!added) {
            sender.sendMessage(ColorUtils.format("&cFailed to force-join player to the Team."));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
            return 1;
        }

        sender.sendMessage(MessagesUtil.get("admin.forcejoin.success")
            .replace("%player%", target.getName())
            .replace("%team%", team.getName()));

        Team updatedTeam = teamManager.getTeamById(team.getId());
        TeamMessengerListener.broadcast(updatedTeam, MessagesUtil.get("admin.forcejoin.notify")
            .replace("%player%", target.getName())
            .replace("%executor%", sender.getName()));

        teamManager.logAudit(
            team.getId(),
            sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
            "Admin Join",
            MessagesUtil.get("admin.forcejoin.audit")
                .replace("%executor%", sender.getName())
                .replace("%player%", target.getName())
        );

        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null) {
            onlineTarget.sendMessage(MessagesUtil.get("admin.forcejoin.you-were-joined")
                .replace("%team%", team.getName())
                .replace("%executor%", sender.getName()));
        }

        Teams.getInstance().getAdminLogManager().logAction(
            sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
            sender.getName(),
            "Force Join",
            team.getName(),
            target.getName(), // or null
            null
        );
        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

        return 1;
    }
}
