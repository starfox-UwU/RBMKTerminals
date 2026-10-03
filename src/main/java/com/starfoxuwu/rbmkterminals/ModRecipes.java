package com.starfoxuwu.rbmkterminals;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.starfoxuwu.rbmkterminals.block.ModBlocks;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModRecipes {

    private ModRecipes() {}

    public static void register() {
        GameRegistry.addRecipe(
            new ItemStack(ModBlocks.displayTerminal),
            new Object[] { "III", "GRG", "III", 'I', Items.iron_ingot, 'G', Blocks.glass, 'R', Items.redstone });

        GameRegistry.addRecipe(
            new ItemStack(ModBlocks.inputTerminal),
            new Object[] { " R ", "BTB", " R ", 'R', Items.redstone, 'B', Blocks.stone_button, 'T',
                ModBlocks.displayTerminal });
    }
}
