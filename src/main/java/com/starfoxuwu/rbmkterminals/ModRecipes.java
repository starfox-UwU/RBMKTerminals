package com.starfoxuwu.rbmkterminals;

import net.minecraft.item.ItemStack;

import com.starfoxuwu.rbmkterminals.block.ModBlocks;
import com.starfoxuwu.rbmkterminals.compat.hbm.HbmCompat;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * One crafting recipe per terminal. Both are placeholders for now, only the shapes and the ingredients are set.
 * <p>
 * The ingredients are HBM's own parts, fetched through {@link HbmCompat}, which answers with HBM's item while HBM is
 * installed and with a vanilla stand-in otherwise, so these grids need not care about that split:
 * <ul>
 * <li>{@code C} - {@code item.circuit.basic}, HBM's integrated circuit board</li>
 * <li>{@code D} - {@code item.crt_display}, HBM's cathode ray tube</li>
 * <li>{@code R} - {@code tile.radio_torch_receiver}, the receiver side of HBM's radio bus</li>
 * <li>{@code S} - {@code tile.radio_torch_sender}, the transmitter side of it</li>
 * </ul>
 * To fill a recipe in, edit just the lines its TODO points at: a shaped recipe is the result, then the pattern - up to
 * three row strings of up to three characters, followed by 'x', ingredient pairs that map a character to an
 * {@code Item}, a {@code Block} or an {@code ItemStack} (use the last one for metadata or stack size, e.g.
 * {@code new ItemStack(someItem, 1, 3)}). Drop a row string to shrink the grid, and note that a bare {@code Block}
 * matches every metadata value of it, which is convenient for HBM's machine blocks.
 */
public final class ModRecipes {

    private ModRecipes() {}

    public static void register() {
        displayTerminal();
        inputTerminal();
    }

    /** The wall mounted screen: a cathode ray tube flanked by radio receivers, it hears what other terminals send. */
    private static void displayTerminal() {
        // TODO placeholder: two basic circuits, one crt display and two radio receivers
        GameRegistry.addRecipe(
            new ItemStack(ModBlocks.displayTerminal),
            new Object[] { "   ", "RDR", "   ", 'C', HbmCompat.basicCircuit(), 'D', HbmCompat.crtDisplay(), 'R',
                HbmCompat.radioReceiver() });
    }

    /** Screen plus keyboard: the display terminal with a pair of transmitters to answer over the radio with. */
    private static void inputTerminal() {
        // TODO placeholder: two transmitters, two basic circuits and one display terminal
        GameRegistry.addRecipe(
            new ItemStack(ModBlocks.inputTerminal),
            new Object[] { "   ", "CTC", " S ", 'S', HbmCompat.radioSender(), 'C', HbmCompat.basicCircuit(), 'T',
                ModBlocks.displayTerminal });
    }
}
