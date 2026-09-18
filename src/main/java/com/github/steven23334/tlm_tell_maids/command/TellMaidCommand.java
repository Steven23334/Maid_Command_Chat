package com.github.steven23334.tlm_tell_maids.command;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;                                     // ★ 新增
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TellMaidCommand {

    private static final double SEARCH_RADIUS = 128.0D;

    private static final DynamicCommandExceptionType ERROR_MAID_NOT_FOUND =
            new DynamicCommandExceptionType(
                    name -> Component.translatable("command.tlm_tell_maids.maid_not_found", name));

    private static final DynamicCommandExceptionType ERROR_NOT_YOUR_MAID =
            new DynamicCommandExceptionType(
                    name -> Component.translatable("command.tlm_tell_maids.not_your_maid", name));

    private static final SimpleCommandExceptionType ERROR_MISSING_MESSAGE =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.missing_message"));

    private static final SimpleCommandExceptionType ERROR_BAD_QUOTE =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.bad_quote"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tellmaid")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests(TellMaidCommand::suggestMaidNames)
                                .executes(TellMaidCommand::exec)
                        )
        );
    }

    /**
     * 从整段输入中解析出 maidName 和 message。
     * 支持：
     *   /tellmaid 酒狐 你好
     *   /tellmaid "My Maid" hello
     */
    private static String[] splitArgs(String args) throws CommandSyntaxException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw ERROR_MISSING_MESSAGE.create();
        }

        String maidName;
        String rest;

        if (trimmed.charAt(0) == '"') {
            int close = trimmed.indexOf('"', 1);
            if (close < 0) {
                throw ERROR_BAD_QUOTE.create();
            }
            maidName = trimmed.substring(1, close);
            rest = trimmed.substring(close + 1).trim();
        } else {
            int space = trimmed.indexOf(' ');
            if (space < 0) {
                throw ERROR_MISSING_MESSAGE.create();
            }
            maidName = trimmed.substring(0, space);
            rest = trimmed.substring(space + 1).trim();
        }

        if (maidName.isEmpty() || rest.isEmpty()) {
            throw ERROR_MISSING_MESSAGE.create();
        }
        return new String[]{maidName, rest};
    }

    /**
     * 补全女仆名字。
     */
    private static CompletableFuture<Suggestions> suggestMaidNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemainingLowerCase();
        String prefix = remaining;
        int space = remaining.indexOf(' ');
        if (space >= 0) {
            prefix = remaining.substring(0, space);
        }

        int quoteCount = 0;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '"') {
                quoteCount++;
            }
        }
        boolean forceQuote = quoteCount >= 1;

        String matchPrefix = prefix.replace("\"", "");

        final String p = matchPrefix;
        final boolean forceQ = forceQuote;

        level.getEntitiesOfClass(EntityMaid.class,
                        player.getBoundingBox().inflate(SEARCH_RADIUS))
                .stream()
                .filter(m -> m.getOwnerUUID() != null
                        && m.getOwnerUUID().equals(player.getUUID()))
                .map(m -> m.getName().getString())
                .filter(name -> name.toLowerCase().startsWith(p))
                .distinct()
                .forEach(name -> {
                    if (forceQ || needsQuoting(name)) {
                        builder.suggest("\"" + name + "\"");
                    } else {
                        builder.suggest(name);
                    }
                });

        return builder.buildFuture();
    }

    private static boolean needsQuoting(String name) {
        if (name.isEmpty()) {
            return true;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean asciiAlnum = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9');
            if (!asciiAlnum && c != '_') {
                return true;
            }
        }
        return false;
    }

    private static int exec(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String args = StringArgumentType.getString(context, "args");
        String[] parsed = splitArgs(args);
        String maidName = parsed[0];
        String messageText = parsed[1];

        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }

        EntityMaid maidEntity = level.getEntitiesOfClass(EntityMaid.class,
                        player.getBoundingBox().inflate(SEARCH_RADIUS))
                .stream()
                .filter(m -> m.getName().getString().equalsIgnoreCase(maidName))
                .findFirst()
                .orElseThrow(() -> ERROR_MAID_NOT_FOUND.create(maidName));

        if (maidEntity.getOwnerUUID() == null
                || !maidEntity.getOwnerUUID().equals(player.getUUID())) {
            throw ERROR_NOT_YOUR_MAID.create(maidName);
        }

        // ★ 在玩家自己的聊天栏显示灰色「已发送」，带玩家名字
        player.sendSystemMessage(
                Component.translatable("chat.tlm_tell_maids.sent",
                                player.getName().getString(), maidName, messageText)
                        .withStyle(ChatFormatting.GRAY));

        maidEntity.getAiChatManager().chat(
                messageText,
                new ChatClientInfo(
                        player.getLanguage(),
                        player.getName().getString(),
                        List.of()
                ),
                player
        );

        return 1;
    }
}