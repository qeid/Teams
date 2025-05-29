package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.database.TeamManager;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
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
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String name = StringArgumentType.getString(ctx, "player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);

                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return 1;
                    }
                    if (target == null || target.getName() == null) {
                        sender.sendMessage(MessagesUtil.get("team.who.not-found"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return 1;
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(Teams.getInstance(), () -> {
                        TeamManager teamManager = Teams.getInstance().getTeamManager();
                        Team team = teamManager.getTeamByPlayer(target.getUniqueId());
                        Bukkit.getScheduler().runTask(Teams.getInstance(), () -> {
                            if (team == null) {
                                sender.sendMessage(MessagesUtil.get("team.who.not-in-team").replace("%player%", name));
                                ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            } else {
                                teamManager.sendTeamInfo(player, team);
                            }
                        });
                    });
                    return 1;
                })).build();
    }
}
