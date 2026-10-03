package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.hbm.main.MainRegistry;

import api.hbm.block.IToolable;

/**
 * The screwdriver aware terminals and the little bit of HBM's tool bookkeeping they need.
 * <p>
 * Everything in here mentions an HBM type, so the class is only loaded once {@link HbmCompat} has established that HBM
 * is installed; the factory methods return plain {@link Block}s so that loading this class is the only thing that can
 * fail, and only when the mod is missing.
 */
final class HbmTerminalBlocks {

    private HbmTerminalBlocks() {}

    static Block displayTerminal() {
        return new BlockHbmDisplayTerminal();
    }

    static Block inputTerminal() {
        return new BlockHbmInputTerminal();
    }

    /** @return HBM's machine tab, the one its own machines, structure parts and RBMK consoles are filed under */
    static CreativeTabs machineTab() {
        return MainRegistry.machineTab;
    }

    /** @return true if the player is holding HBM's screwdriver */
    static boolean holdsScrewdriver(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        return held != null && drivesConfiguration(IToolable.ToolType.getType(held));
    }

    /** @return true if this tool is the one the configuration window is bound to */
    static boolean drivesConfiguration(IToolable.ToolType tool) {
        return tool == IToolable.ToolType.SCREWDRIVER;
    }
}
