package discord.qeid.commands.teams;

import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.mojang.brigadier.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamHomeCommand {

    /*    * This command allows players to teleport to their team's home location.
     * It checks for a cooldown and whether the player is in a team.
     * If the home location is not set, it informs the player.
     * If successful, it teleports the player and plays a success sound.
     */

    // Map<UUID, lastHomeTpMillis>
    private static final Map<UUID, Long> lastHomeTp = new HashMap<>();

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("home")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessagesUtil.get("general.players-only"));
                    return Command.SINGLE_SUCCESS;
                }

                FileConfiguration config = Teams.getInstance().getConfig();
                int cooldownSeconds = config.getInt("team.home.cooldown-seconds", 60);

                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                long last = lastHomeTp.getOrDefault(uuid, 0L);
                long wait = (last + cooldownSeconds * 1000L) - now;
                if (wait > 0) {
                    long seconds = wait / 1000;
                    player.sendMessage(MessagesUtil.get("team.home.cooldown")
                        .replace("%cooldown%", String.valueOf(seconds)));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                var teamManager = Teams.getInstance().getTeamManager();
                Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }
                Location home = teamManager.getHome(team.getId());
                if (home == null) {
                    player.sendMessage(MessagesUtil.get("team.home.not-set"));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }

                lastHomeTp.put(uuid, now);

                player.teleport(home);
                player.sendMessage(MessagesUtil.get("team.home.success"));
                player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);
                return Command.SINGLE_SUCCESS;
            }).build();
    }
}
