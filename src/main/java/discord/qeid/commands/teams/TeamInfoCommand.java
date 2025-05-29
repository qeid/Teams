package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;

import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamInfoCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("info")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtils.format("&cOnly players can run this."));
                    return 1;
                }
                UUID uuid = player.getUniqueId();
                Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
                    TeamManager teamManager = Teams.getInstance().getTeamManager();
                    Team team = teamManager.getTeamByPlayer(uuid);
                    Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                        if (team == null) {
                            player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                            player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        } else {
                            teamManager.sendTeamInfo(player, team);
                        }
                    });
                });
                return 1;
            })
            .then(Commands.argument("team", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    Teams plugin = Teams.getInstance();
                    var teamManager = plugin.getTeamManager();
                    teamManager.getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return 1;
                    }
                    String input = StringArgumentType.getString(ctx, "team");
                    Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
                        TeamManager teamManager = Teams.getInstance().getTeamManager();
                        Team team = teamManager.getTeamByName(input);
                        Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                            if (team == null) {
                                player.sendMessage(MessagesUtil.get("team.info.team-not-found"));
                                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            } else {
                                teamManager.sendTeamInfo(player, team);
                            }
                        });
                    });
                    return 1;
                })
            ).build();
    }




    public static String formatPlayerList(Set<UUID> players) {
        if (players.isEmpty()) return MessagesUtil.get("team.null");
        return players.stream()
            .map(TeamInfoCommand::formatPlayerName)
            .collect(Collectors.joining("&r, "));
    }

    public static String formatPlayerName(UUID uuid) {
        boolean showIndicator = Teams.getInstance().getConfig().getBoolean(
            "team.info.show-online-indicator", true);

        Player p = Bukkit.getPlayer(uuid);
        String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();

        if (!showIndicator) {
            return name;
        }

        String dot = (p != null && p.isOnline())
            ? MessagesUtil.get("team.info.online-indicator")
            : MessagesUtil.get("team.info.offline-indicator");
        return name + " " + dot;
    }
}
