package discord.qeid.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord.qeid.Teams;
import discord.qeid.utils.MessagesUtil;
import discord.qeid.utils.SoundUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamAdminReloadCommand {

    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("reload")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                String perm = Teams.getInstance().getConfig().getString("admin.permission", "teams.admin");
                if (!sender.hasPermission(perm)) {
                    sender.sendMessage(MessagesUtil.get("admin.reload.no-permission"));
                    ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    return Command.SINGLE_SUCCESS;
                }
                long start = System.currentTimeMillis();
                try {
                    MessagesUtil.loadMessages(Teams.getInstance());
                    Teams.getInstance().reloadConfig();
                    long time = System.currentTimeMillis() - start;
                    sender.sendMessage(MessagesUtil.get("admin.reload.success")
                        .replace("%type%", "messages & config")
                        .replace("%time%", String.valueOf(time)));
                    ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                } catch (Exception e) {
                    e.printStackTrace();
                    sender.sendMessage(MessagesUtil.get("admin.reload.failed")
                        .replace("%type%", "messages & config"));
                    ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                }
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    builder.suggest("database");
                    builder.suggest("messages");
                    builder.suggest("config");
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String perm = Teams.getInstance().getConfig().getString(
                        "admin.permission", "teams.admin"
                    );
                    if (!sender.hasPermission(perm)) {
                        sender.sendMessage(MessagesUtil.get("admin.reload.no-permission"));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                        return Command.SINGLE_SUCCESS;
                    }

                    String type = StringArgumentType.getString(ctx, "type").toLowerCase();
                    long start = System.currentTimeMillis();
                    boolean success = false;

                    switch (type) {
                        case "database" -> {
                            try {
                                Teams.getInstance().getDatabaseManager().close();
                                Teams.getInstance().getDatabaseManager().connect();
                                Teams.getInstance().getDatabaseManager().initializeSchema();
                                success = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            }
                        }
                        case "messages" -> {
                            try {
                                MessagesUtil.loadMessages(Teams.getInstance());
                                success = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            }
                        }
                        case "config" -> {
                            try {
                                Teams.getInstance().reloadConfig();
                                success = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            }
                        }
                        default -> {
                            sender.sendMessage(MessagesUtil.get("admin.reload.usage"));
                            ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                            return Command.SINGLE_SUCCESS;
                        }
                    }

                    long time = System.currentTimeMillis() - start;
                    if (success) {
                        sender.sendMessage(MessagesUtil.get("admin.reload.success")
                            .replace("%type%", type)
                            .replace("%time%", String.valueOf(time)));
                    } else {
                        sender.sendMessage(MessagesUtil.get("admin.reload.failed")
                            .replace("%type%", type));
                        ((Player)sender).playSound(((Player)sender).getLocation(), SoundUtil.get("team.sounds.error"), 1.0F, 1.5F);
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
