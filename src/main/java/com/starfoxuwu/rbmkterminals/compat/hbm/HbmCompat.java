package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.starfoxuwu.rbmkterminals.block.BlockDisplayTerminal;
import com.starfoxuwu.rbmkterminals.block.BlockInputTerminal;

import cpw.mods.fml.common.Loader;

/**
 * Entry point of the optional HBM integration: it hands {@code ModBlocks} the version of a terminal that HBM's
 * screwdriver can talk to, or the ordinary one when HBM is not installed.
 * <p>
 * HBM's screwdriver is a plain item ({@code com.hbm.items.tool.ItemTooling}) that looks for
 * {@code api.hbm.block.IToolable} on the clicked block and calls {@code onScrew}, which is exactly the hook
 * {@code tile.rbmk_indicator} uses for its own configuration window. Implementing that interface puts HBM's types into
 * a class, so those classes live in {@link HbmTerminalBlocks} and this factory - which knows nothing but the mod id -
 * is what the rest of the mod talks to. That way the terminals stay loadable on a game without HBM.
 */
public final class HbmCompat {

    public static final String MODID = "hbm";

    private HbmCompat() {}

    /** @return true while HBM's Nuclear Tech Mod is installed */
    public static boolean isAvailable() {
        return Loader.isModLoaded(MODID);
    }

    /** @return the display terminal to register */
    public static Block createDisplayTerminal() {
        return isAvailable() ? HbmTerminalBlocks.displayTerminal() : new BlockDisplayTerminal();
    }

    /** @return the input terminal to register */
    public static Block createInputTerminal() {
        return isAvailable() ? HbmTerminalBlocks.inputTerminal() : new BlockInputTerminal();
    }

    /**
     * Files a terminal under HBM's machine tab ({@code itemGroup.tabMachine}), the same page the RBMK consoles and the
     * radio equipment a terminal talks to are on. On a game without HBM the block keeps the tab its constructor picked,
     * so the terminals stay reachable either way.
     */
    public static void applyMachineTab(Block block) {
        if (isAvailable()) block.setCreativeTab(HbmTerminalBlocks.machineTab());
    }

    /**
     * Hooks the terminals up to HBM's own radio bus.
     * <p>
     * HBM's {@code RTTYSystem} is a network of its own, so without this step no terminal would ever see what
     * {@code tile.radio_autocal} sends, nor would anything HBM has hear a terminal. {@link HbmRORBus} is the only
     * class that talks to it; on a game without HBM this is a no-op and none of that is loaded.
     */
    public static void installRORBus() {
        if (isAvailable()) HbmRORBus.install();
    }

    /*
     * The four parts the terminal recipes are built from. Each one answers with HBM's own item while HBM is installed
     * and with a plain vanilla stand-in otherwise, which keeps the recipe grids in one piece and still leaves the
     * terminals craftable on a game without HBM.
     */

    /** @return HBM's integrated circuit board ({@code item.circuit.basic}), or redstone without HBM */
    public static ItemStack basicCircuit() {
        return isAvailable() ? HbmTerminalParts.basicCircuit() : new ItemStack(Items.redstone);
    }

    /** @return HBM's cathode ray tube ({@code item.crt_display}), or glass without HBM */
    public static ItemStack crtDisplay() {
        return isAvailable() ? HbmTerminalParts.crtDisplay() : new ItemStack(Blocks.glass);
    }

    /**
     * @return HBM's Redstone-over-Radio receiver ({@code tile.radio_torch_receiver}), or a redstone torch without HBM
     */
    public static ItemStack radioReceiver() {
        return isAvailable() ? HbmTerminalParts.radioReceiver() : new ItemStack(Blocks.redstone_torch);
    }

    /** @return HBM's Redstone-over-Radio transmitter ({@code tile.radio_torch_sender}), or a comparator without HBM */
    public static ItemStack radioSender() {
        return isAvailable() ? HbmTerminalParts.radioSender() : new ItemStack(Items.comparator);
    }
}
