package discord.qeid.commands.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.listeners.TeamMessengerListener;
import discord.qeid.model.Team;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

import static discord.qeid.utils.ColorUtils.applyTagCase;

public class TeamTagCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("tag")
            .then(Commands.argument("new tag", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(MessagesUtil.get("general.players-only"));
                        return Command.SINGLE_SUCCESS;
                    }

                    FileConfiguration config = Teams.getInstance().getConfig();

                    int minTagLength = config.getInt("team.create.min-tag-length", 2);
                    int maxTagLength = config.getInt("team.create.max-tag-length", 5);
                    String tagRegex = config.getString("team.create.tag-regex", "^[A-Za-z0-9]+$");
                    String tagCase = config.getString("team.create.tag-case", "upper");

                    String newTag = StringArgumentType.getString(ctx, "new tag").trim();
                    newTag = applyTagCase(newTag, tagCase);

                    var teamManager = Teams.getInstance().getTeamManager();
                    Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                    if (team == null) {
                        player.sendMessage(MessagesUtil.get("team.info.not-in-team"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!team.getOwner().equals(player.getUniqueId())) {
                        player.sendMessage(MessagesUtil.get("team.tag.no-permission"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (newTag.length() < minTagLength || newTag.length() > maxTagLength ||
                        !Pattern.matches(tagRegex, newTag)) {
                        player.sendMessage(MessagesUtil.get("team.tag.invalid")
                            .replace("%min%", String.valueOf(minTagLength))
                            .replace("%max%", String.valueOf(maxTagLength))
                            .replace("%regex%", tagRegex));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    Team existing = teamManager.getTeamByTag(newTag);
                    if (existing != null && !existing.getId().equals(team.getId())) {
                        player.sendMessage(MessagesUtil.get("team.tag.exists")
                            .replace("%tag%", newTag));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    boolean success = teamManager.setTag(team.getId(), newTag);
                    if (!success) {
                        player.sendMessage(MessagesUtil.get("team.tag.failed"));
                        player.playSound(player.getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    Team updatedTeam = teamManager.getTeamById(team.getId());
                    TeamMessengerListener.broadcastWithRank(updatedTeam, player.getUniqueId(),
                        MessagesUtil.get("team.notifications.tag-change")
                            .replace("%executor%", player.getName())
                            .replace("%tag%", newTag)
                    );

                    player.sendMessage(MessagesUtil.get("team.tag.success")
                        .replace("%tag%", newTag));
                    player.playSound(player.getLocation(), SoundUtil.get("team.sounds.success"), 1.0F, 1.5F);

                    teamManager.logAudit(
                        team.getId(),
                        player.getUniqueId(),
                        "Tag Change",
                        player.getName() + " changed the team tag to [" + newTag + "]."
                    );

                    return Command.SINGLE_SUCCESS;
                })
            ).build();
    }
}
