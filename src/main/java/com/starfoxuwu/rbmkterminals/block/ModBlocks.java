package com.starfoxuwu.rbmkterminals.block;

import net.minecraft.block.Block;

import com.starfoxuwu.rbmkterminals.compat.hbm.HbmCompat;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityDisplayTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static Block displayTerminal;
    public static Block inputTerminal;

    private ModBlocks() {}

    public static void register() {
        // with HBM installed these are the versions its screwdriver can reconfigure, see HbmCompat
        displayTerminal = HbmCompat.createDisplayTerminal()
            .setBlockName("display_terminal");
        inputTerminal = HbmCompat.createInputTerminal()
            .setBlockName("input_terminal");

        // with HBM installed both terminals join its machine tab, next to the RBMK consoles they are modelled after
        HbmCompat.applyMachineTab(displayTerminal);
        HbmCompat.applyMachineTab(inputTerminal);

        GameRegistry.registerBlock(displayTerminal, ItemBlockTerminal.class, "display_terminal");
        GameRegistry.registerBlock(inputTerminal, ItemBlockTerminal.class, "input_terminal");

        GameRegistry.registerTileEntity(TileEntityDisplayTerminal.class, "rbmkterminals:display_terminal");
        GameRegistry.registerTileEntity(TileEntityInputTerminal.class, "rbmkterminals:input_terminal");
    }
}
