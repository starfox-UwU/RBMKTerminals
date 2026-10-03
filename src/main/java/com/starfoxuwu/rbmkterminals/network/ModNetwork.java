package com.starfoxuwu.rbmkterminals.network;

import com.starfoxuwu.rbmkterminals.RBMKTerminals;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    /** GUI id of the input terminal's keyboard, see {@code TerminalGuiHandler}. */
    public static final int GUI_INPUT_TERMINAL = 0;

    /** GUI id of the screwdriver's configuration window, see {@code TerminalGuiHandler}. */
    public static final int GUI_TERMINAL_CONFIG = 1;

    private static SimpleNetworkWrapper channel;

    private ModNetwork() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(RBMKTerminals.MODID);
        channel.registerMessage(PacketTerminalCommand.Handler.class, PacketTerminalCommand.class, 0, Side.SERVER);
        channel.registerMessage(PacketTerminalConfig.Handler.class, PacketTerminalConfig.class, 1, Side.SERVER);
    }

    public static SimpleNetworkWrapper getChannel() {
        return channel;
    }
}
