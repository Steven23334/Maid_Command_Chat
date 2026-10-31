package com.github.steven23334.tlm_tell_maids;

import com.github.steven23334.tlm_tell_maids.command.MaidTargetArgument;
import com.github.steven23334.tlm_tell_maids.command.MaidTargetArgumentInfo;
import com.github.steven23334.tlm_tell_maids.command.TellMaidCommand;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(TlmTellMaids.MOD_ID)
public class TlmTellMaids {
    public static final String MOD_ID = "tlm_tell_maids";

    public TlmTellMaids(IEventBus modBus) {
        modBus.addListener(this::onRegisterArgumentTypes);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    /**
     * 注册自定义的“女仆”参数类型，让命令树能同步到客户端（否则客户端没有补全）。
     */
    private void onRegisterArgumentTypes(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.COMMAND_ARGUMENT_TYPE)) {
            return;
        }
        event.register(Registries.COMMAND_ARGUMENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "maid_target"),
                () -> MaidTargetArgumentInfo.INSTANCE);
        ArgumentTypeInfos.registerByClass(MaidTargetArgument.class, MaidTargetArgumentInfo.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TellMaidCommand.register(event.getDispatcher());
    }
}
