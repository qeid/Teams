package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ColorUtils;
import discord.qeid.utils.MessagesUtil;

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
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    return Command.SINGLE_SUCCESS;
                }

                teamManager.sendTeamInfo(player, team);
                return Command.SINGLE_SUCCESS;
            })

            .then(Commands.argument("team", StringArgumentType.greedyString())
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
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    String input = StringArgumentType.getString(ctx, "team");
                    TeamManager teamManager = Teams.getInstance().getTeamManager();
                    Team team = teamManager.getTeamByName(input);

                    if (team == null) {
                        player.sendMessage(MessagesUtil.get("team.info.team-not-found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    teamManager.sendTeamInfo(player, team);
                    return Command.SINGLE_SUCCESS;
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
