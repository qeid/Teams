package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.ConfigUtil;

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
                    return Command.SINGLE_SUCCESS;
                }

                UUID uuid = player.getUniqueId();
                TeamManager teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(uuid);

                if (team == null) {
                    player.sendMessage(ConfigUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }

                teamManager.sendTeamInfo(player, team);
                return Command.SINGLE_SUCCESS;
            })

            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    //System.out.println("[DEBUG][INFO] Tab suggestions requested: ");

                    Teams plugin = Teams.getInstance();
                    var teamManager = plugin.getTeamManager();

                    teamManager.getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        //.peek(name -> System.out.println("[DEBUG][INFO] Suggesting: " + name))
                        .forEach(builder::suggest);

                    return builder.buildFuture();
                })

                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ConfigUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String input = StringArgumentType.getString(ctx, "team");
                    TeamManager teamManager = Teams.getInstance().getTeamManager();
                    Team team = teamManager.getTeamByName(input);

                    if (team == null) {
                        player.sendMessage(ConfigUtil.get("team.info.team-not-found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    teamManager.sendTeamInfo(player, team);
                    return Command.SINGLE_SUCCESS;
                })
            ).build();

}



    public static String formatPlayerList(Set<UUID> players) {
        if (players.isEmpty()) return "&8(None)";
        return players.stream()
            .map(TeamInfoCommand::formatPlayerName)
            .collect(Collectors.joining("&r, "));
    }

    public static String formatPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        String dot = (p != null && p.isOnline()) ? ConfigUtil.get("team.info.online-indicator") : ConfigUtil.get("team.info.offline-indicator");
        String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
        return name + " " + dot;
    }
}
