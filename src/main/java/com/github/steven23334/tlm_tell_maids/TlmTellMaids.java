package com.github.steven23334.tlm_tell_maids;

import com.github.steven23334.tlm_tell_maids.command.TellMaidCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(TlmTellMaids.MOD_ID)
public class TlmTellMaids {
    public static final String MOD_ID = "tlm_tell_maids";

    public TlmTellMaids(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TellMaidCommand.register(event.getDispatcher());
    }
}