package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.ConfigUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamWhoCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("who")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String name = StringArgumentType.getString(ctx, "player");

                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);

                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ConfigUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (target == null || target.getName() == null) {
                        sender.sendMessage(ConfigUtil.get("team.who.not-found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    Team team = Teams.getInstance().getTeamManager().getTeamByPlayer(target.getUniqueId());
                    if (team == null) {
                        sender.sendMessage(ConfigUtil.get("team.who.not-in-team").replace("%player%", name));
                        return Command.SINGLE_SUCCESS;
                    }

                    TeamManager.sendTeamInfo(player, team);
                    return Command.SINGLE_SUCCESS;
                })).build();
    }
}
