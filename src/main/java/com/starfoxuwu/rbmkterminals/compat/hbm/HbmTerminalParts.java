package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.item.ItemStack;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemCircuit.EnumCircuitType;

/**
 * The HBM parts the terminal recipes are made of, named after the keys they show up as in game:
 * {@code item.circuit.basic}, {@code item.crt_display}, {@code tile.radio_torch_receiver} and
 * {@code tile.radio_torch_sender}.
 * <p>
 * Just like {@link HbmTerminalBlocks} this class mentions HBM types, so it is only ever loaded once {@link HbmCompat}
 * has established that HBM is installed.
 */
final class HbmTerminalParts {

    private HbmTerminalParts() {}

    /** The basic integrated circuit board; HBM keeps all of its circuits as metadata on the one item. */
    static ItemStack basicCircuit() {
        return new ItemStack(ModItems.circuit, 1, EnumCircuitType.BASIC.ordinal());
    }

    /** The cathode ray tube HBM builds its consoles and screens from. */
    static ItemStack crtDisplay() {
        return new ItemStack(ModItems.crt_display);
    }

    /** Redstone-over-Radio receiver, the one that turns radio traffic back into redstone. */
    static ItemStack radioReceiver() {
        return new ItemStack(ModBlocks.radio_torch_receiver);
    }

    /** Redstone-over-Radio transmitter, the one that puts redstone onto the air. */
    static ItemStack radioSender() {
        return new ItemStack(ModBlocks.radio_torch_sender);
    }
}
