package com.github.steven23334.tlm_tell_maids.command;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TellMaidCommand {

    private static final double SEARCH_RADIUS = 128.0D;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DynamicCommandExceptionType ERROR_MAID_NOT_FOUND =
            new DynamicCommandExceptionType(
                    name -> Component.translatable("command.tlm_tell_maids.maid_not_found", name));

    private static final DynamicCommandExceptionType ERROR_NOT_YOUR_MAID =
            new DynamicCommandExceptionType(
                    name -> Component.translatable("command.tlm_tell_maids.not_your_maid", name));

    private static final SimpleCommandExceptionType ERROR_MISSING_MESSAGE =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.missing_message"));

    private static final SimpleCommandExceptionType ERROR_NO_MAID_OWNED =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.no_maid_owned", (int) SEARCH_RADIUS));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tellmaid")
                        .requires(source -> source.hasPermission(0))
                        // /tellmaid <女仆> <消息>
                        .then(Commands.argument("maid", MaidTargetArgument.target())
                                .suggests(TellMaidCommand::suggest)
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(TellMaidCommand::exec)
                                )
                        )
        );
    }

    /**
     * 补全女仆名、{@code @m} 以及 {@code @m[...]} 列表。
     *
     * <p>{@code @m[...]} 内的候选只替换当前正在输入的那一段（和原版 {@code @e[...]} 一样），
     * 所以候选栏里显示的是单纯的女仆名，而不是整段 {@code @m[...]}。
     */
    private static CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return builder.buildFuture();
        }

        List<String> maidNames = getNearbyOwnedMaidNames(level, player);
        List<MaidCommandInput.Suggestion> completions =
                MaidCommandInput.suggestions(builder.getRemaining(), maidNames);
        if (completions.isEmpty()) {
            return builder.buildFuture();
        }

        int argumentStart = builder.getStart();
        int inputLength = builder.getInput().length();
        int replaceStart = argumentStart + completions.get(0).start();
        StringRange range = StringRange.between(replaceStart, inputLength);

        List<Suggestion> suggestions = new ArrayList<>(completions.size());
        for (MaidCommandInput.Suggestion completion : completions) {
            suggestions.add(new Suggestion(
                    StringRange.between(argumentStart + completion.start(), inputLength),
                    completion.text()));
        }
        return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
    }

    private static List<String> getNearbyOwnedMaidNames(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(EntityMaid.class,
                        player.getBoundingBox().inflate(SEARCH_RADIUS))
                .stream()
                .filter(EntityMaid::isAlive)
                .filter(maid -> isOwnedBy(maid, player))
                .map(maid -> maid.getName().getString())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static int exec(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MaidCommandInput.Target target = MaidTargetArgument.get(context);
        String message = StringArgumentType.getString(context, "message");
        if (message.isBlank()) {
            throw ERROR_MISSING_MESSAGE.create();
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }

        List<EntityMaid> nearby = level.getEntitiesOfClass(EntityMaid.class,
                player.getBoundingBox().inflate(SEARCH_RADIUS));
        List<EntityMaid> targets = resolveTargets(target, nearby, player);

        String targetNames = targets.stream()
                .map(maid -> maid.getName().getString())
                .collect(Collectors.joining(", "));

        // 先回显本次选中的女仆，保证任何情况下玩家都能在聊天栏看到结果
        player.sendSystemMessage(
                Component.translatable("chat.tlm_tell_maids.sent",
                                player.getName().getString(), targetNames, message)
                        .withStyle(ChatFormatting.GRAY));

        // AI 聊天被关闭时只提示一次，避免 @m 选中多个女仆时刷屏
        if (!AIConfig.LLM_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.disable")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ChatClientInfo clientInfo = new ChatClientInfo(
                player.getLanguage(),
                player.getName().getString(),
                List.of()
        );

        // 每个女仆都有独立的 AI 对话上下文，这里逐个触发，效果就是“同时和她们说话”
        int started = 0;
        for (EntityMaid maid : targets) {
            try {
                maid.getAiChatManager().chat(message, clientInfo, player);
                started++;
            } catch (Exception e) {
                // 单个女仆出错时也要在聊天栏说明，并且不影响其他女仆
                String maidName = maid.getName().getString();
                LOGGER.error("Failed to start AI chat with maid {}", maidName, e);
                player.sendSystemMessage(
                        Component.translatable("command.tlm_tell_maids.chat_failed", maidName)
                                .withStyle(ChatFormatting.RED));
            }
        }
        return started;
    }

    private static List<EntityMaid> resolveTargets(
            MaidCommandInput.Target target,
            List<EntityMaid> nearby,
            ServerPlayer player) throws CommandSyntaxException {

        Map<UUID, EntityMaid> targets = new LinkedHashMap<>();

        if (target.all()) {
            nearby.stream()
                    .filter(EntityMaid::isAlive)
                    .filter(maid -> isOwnedBy(maid, player))
                    .sorted(Comparator.comparingDouble(maid -> maid.distanceToSqr(player)))
                    .forEach(maid -> targets.putIfAbsent(maid.getUUID(), maid));
            if (targets.isEmpty()) {
                throw ERROR_NO_MAID_OWNED.create();
            }
        } else {
            for (String name : target.names()) {
                EntityMaid maid = nearby.stream()
                        .filter(EntityMaid::isAlive)
                        .filter(entity -> entity.getName().getString().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseThrow(() -> ERROR_MAID_NOT_FOUND.create(name));
                if (!isOwnedBy(maid, player)) {
                    throw ERROR_NOT_YOUR_MAID.create(name);
                }
                targets.putIfAbsent(maid.getUUID(), maid);
            }
        }

        return new ArrayList<>(targets.values());
    }

    private static boolean isOwnedBy(EntityMaid maid, ServerPlayer player) {
        return maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(player.getUUID());
    }
}
