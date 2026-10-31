package com.github.steven23334.tlm_tell_maids.command;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 让 {@link MaidTargetArgument} 能被同步到客户端（补全、命令树都需要）。
 */
public final class MaidTargetArgumentInfo
        implements ArgumentTypeInfo<MaidTargetArgument, MaidTargetArgumentInfo.Template> {

    public static final MaidTargetArgumentInfo INSTANCE = new MaidTargetArgumentInfo();

    private MaidTargetArgumentInfo() {
    }

    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
        return new Template();
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {
    }

    @Override
    public Template unpack(MaidTargetArgument argument) {
        return new Template();
    }

    public static final class Template implements ArgumentTypeInfo.Template<MaidTargetArgument> {
        @Override
        public MaidTargetArgument instantiate(CommandBuildContext context) {
            return MaidTargetArgument.target();
        }

        @Override
        public ArgumentTypeInfo<MaidTargetArgument, ?> type() {
            return INSTANCE;
        }
    }
}
