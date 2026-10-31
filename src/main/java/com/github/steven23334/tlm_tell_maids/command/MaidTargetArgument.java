package com.github.steven23334.tlm_tell_maids.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

/**
 * {@code /tellmaid <女仆> <消息>} 里的“女仆”参数。
 *
 * <p>它只读取一个目标：普通名字（可加引号）或 {@code @m} / {@code @m[...]}。
 * 解析失败（引号/方括号未闭合、空名字、未知选择器）会直接抛出红色命令错误，
 * 与原版“命令不完整”一样拒绝执行。
 */
public final class MaidTargetArgument implements ArgumentType<MaidCommandInput.Target> {
    private static final MaidTargetArgument INSTANCE = new MaidTargetArgument();

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR =
            new DynamicCommandExceptionType(
                    selector -> Component.translatable("command.tlm_tell_maids.unknown_selector", selector));

    private static final SimpleCommandExceptionType ERROR_BAD_QUOTE =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.bad_quote"));

    private static final SimpleCommandExceptionType ERROR_BAD_SEPARATOR =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.bad_separator"));

    private static final SimpleCommandExceptionType ERROR_EMPTY_NAME =
            new SimpleCommandExceptionType(
                    Component.translatable("command.tlm_tell_maids.empty_name"));

    private MaidTargetArgument() {
    }

    public static MaidTargetArgument target() {
        return INSTANCE;
    }

    public static MaidCommandInput.Target get(CommandContext<?> context) {
        return context.getArgument("maid", MaidCommandInput.Target.class);
    }

    @Override
    public MaidCommandInput.Target parse(StringReader reader) throws CommandSyntaxException {
        try {
            MaidCommandInput.TargetParse parsed =
                    MaidCommandInput.parseTarget(reader.getString(), reader.getCursor());
            reader.setCursor(parsed.end());
            return parsed.target();
        } catch (MaidCommandInput.ParseException e) {
            throw toCommandException(e);
        }
    }

    private static CommandSyntaxException toCommandException(MaidCommandInput.ParseException e) {
        return switch (e.error()) {
            case BAD_QUOTE -> ERROR_BAD_QUOTE.create();
            case BAD_SEPARATOR -> ERROR_BAD_SEPARATOR.create();
            case UNKNOWN_SELECTOR -> ERROR_UNKNOWN_SELECTOR.create(e.detail());
            case EMPTY_NAME -> ERROR_EMPTY_NAME.create();
        };
    }
}
