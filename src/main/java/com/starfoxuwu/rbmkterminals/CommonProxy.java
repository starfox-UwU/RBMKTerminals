package com.starfoxuwu.rbmkterminals;

import com.starfoxuwu.rbmkterminals.block.ModBlocks;
import com.starfoxuwu.rbmkterminals.command.CommandTerminal;
import com.starfoxuwu.rbmkterminals.compat.hbm.HbmCompat;
import com.starfoxuwu.rbmkterminals.network.ModNetwork;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        RBMKTerminals.LOG.info(Config.greeting);
        RBMKTerminals.LOG.info("I am RBMKTerminals at version " + Tags.VERSION);

        ModBlocks.register();
        // with HBM installed both mods share one radio network, so a terminal hears tile.radio_autocal
        HbmCompat.installRORBus();
        ModNetwork.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(RBMKTerminals.instance, new TerminalGuiHandler());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTerminal());
    }

    /** @return the terminal keyboard, or null on a dedicated server where there is nothing to show it on */
    public Object getTerminalGui(TileEntityInputTerminal terminal) {
        return null;
    }

    /** @return the screwdriver's configuration window, or null on a dedicated server */
    public Object getTerminalConfigGui(TileEntityTerminalBase terminal) {
        return null;
    }
}
