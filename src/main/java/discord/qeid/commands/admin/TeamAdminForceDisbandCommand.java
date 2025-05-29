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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamAdminForceDisbandCommand {

    private static final Map<UUID, PendingDisband> pending = new HashMap<>();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("disband")
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    Teams.getInstance().getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String teamName = StringArgumentType.getString(ctx, "team");
                        String reason = StringArgumentType.getString(ctx, "reason");
                        return execute(ctx.getSource().getSender(), teamName, reason);
                    })
                )
            ).build();
    }

    private static int execute(CommandSender sender, String teamName, String reason) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.format("&cOnly players can use this command."));
            return 1;
        }
        UUID adminId = player.getUniqueId();
        TeamManager teamManager = Teams.getInstance().getTeamManager();
        Team team = teamManager.getTeamByName(teamName);

        if (team == null) {
            sender.sendMessage(MessagesUtil.get("admin.forcedisband.not-found"));
            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);

            return 1;
        }

        long now = System.currentTimeMillis();
        PendingDisband pendingDisband = pending.get(adminId);

        if (pendingDisband != null
                && pendingDisband.teamName.equalsIgnoreCase(teamName)
                && pendingDisband.reason.equals(reason)
                && now - pendingDisband.timestamp <= 10_000) {

            pending.remove(adminId);


            TeamMessengerListener.broadcast(team, MessagesUtil.get("admin.forcedisband.notify")
                .replace("%executor%", sender.getName())
                .replace("%reason%", reason));


            Teams.getInstance().getAdminLogManager().logAction(
                adminId,
                sender.getName(),
                "Force Disband",
                team.getName(),
                null,
                reason
            );


            boolean deleted = teamManager.disbandTeam(team.getId());
            if (!deleted) {
                sender.sendMessage(MessagesUtil.get("admin.forcedisband.failed"));
                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                return 1;
            }

            sender.sendMessage(MessagesUtil.get("admin.forcedisband.success")
                .replace("%team%", team.getName())
                .replace("%reason%", reason));
            ((Player)sender).playSound(((Player)sender).getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0F, 1.5F);
            return 1;
        }


        pending.put(adminId, new PendingDisband(teamName, reason, now));
        sender.sendMessage(MessagesUtil.get("admin.forcedisband.confirm")
            .replace("%team%", teamName)
            .replace("%reason%", reason));
        ((Player)sender).playSound(((Player)sender).getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0F, 1.5F);


        Bukkit.getScheduler().runTaskLater(Teams.getInstance(), () -> {
            PendingDisband pd = pending.get(adminId);
            if (pd != null && pd.teamName.equalsIgnoreCase(teamName) && pd.reason.equals(reason)) {
                pending.remove(adminId);
                sender.sendMessage(MessagesUtil.get("admin.forcedisband.timeout"));
                ((Player)sender).playSound(((Player)sender).getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
            }
        }, 200L); // 10 seconds

        return 1;
    }

    private static class PendingDisband {
        final String teamName;
        final String reason;
        final long timestamp;
        PendingDisband(String teamName, String reason, long timestamp) {
            this.teamName = teamName;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}
