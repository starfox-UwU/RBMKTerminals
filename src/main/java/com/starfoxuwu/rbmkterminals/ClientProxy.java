package com.starfoxuwu.rbmkterminals;

import com.starfoxuwu.rbmkterminals.client.GuiTerminal;
import com.starfoxuwu.rbmkterminals.client.GuiTerminalConfig;
import com.starfoxuwu.rbmkterminals.client.RenderTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityDisplayTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        RenderTerminal renderer = new RenderTerminal();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDisplayTerminal.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityInputTerminal.class, renderer);
    }

    @Override
    public Object getTerminalGui(TileEntityInputTerminal terminal) {
        return new GuiTerminal(terminal);
    }

    @Override
    public Object getTerminalConfigGui(TileEntityTerminalBase terminal) {
        return new GuiTerminalConfig(terminal);
    }
}
