package discord.qeid.commands.teams;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import discord.qeid.utils.ColorUtils;

import org.bukkit.command.CommandSender;
import discord.qeid.utils.MessagesUtil;

import java.util.List;

public class TeamHelpCommand {



    public static LiteralCommandNode<CommandSourceStack> buildSubcommand() {
        return Commands.literal("help")
            .executes(ctx -> runHelp(ctx, 1))
            .then(Commands.argument("page number", IntegerArgumentType.integer(1, 2))
                .executes(ctx -> {
                    int page = IntegerArgumentType.getInteger(ctx, "page number");
                    return runHelp(ctx, page);
                })
            ).build();
    }

    public static int runHelpDefault(CommandContext<CommandSourceStack> ctx) {
        return runHelp(ctx, 1);
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSender sender = ctx.getSource().getSender();

        List<String> lines = MessagesUtil.getMessages().getStringList("team.help.page-" + page);
        if (lines.isEmpty()) {
            sender.sendMessage(ColorUtils.format("§cNo help message found for page " + page + "."));
            return 1;
        }

        sender.sendMessage(MessagesUtil.get("team.help.header").replace("%page%", page + ""));
        for (String line : lines) {
            sender.sendMessage(ColorUtils.format(line));
        }

        return 1;
    }
}
