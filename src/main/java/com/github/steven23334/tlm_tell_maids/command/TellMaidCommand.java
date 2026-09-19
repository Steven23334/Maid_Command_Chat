package com.github.steven23334.tlm_tell_maids.command;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tellmaid")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.argument("maid", StringArgumentType.string())
                                .suggests(TellMaidCommand::suggestMaidNames)
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(TellMaidCommand::exec)
                                )
                        )
        );
    }

    /**
     * 补全女仆名字。
     * 名字含非 ASCII 字符（中文、破折号、空格等）时补成带引号的形式。
     */
    private static CompletableFuture<Suggestions> suggestMaidNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return builder.buildFuture();
        }

        String prefix = builder.getRemainingLowerCase();
        boolean forceQuote = prefix.startsWith("\"");
        if (forceQuote) {
            prefix = prefix.substring(1);
        }

        final String p = prefix;
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

    /**
     * 判断名字是否超出 Brigadier 默认 word 字符集 [a-zA-Z0-9_]，
     * 超出则需要用双引号包裹才能被命令解析。
     */
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
        String maidName = StringArgumentType.getString(context, "maid");
        String messageText = StringArgumentType.getString(context, "message");

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

        // 在玩家自己的聊天栏显示灰色「已发送」，带玩家名字
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