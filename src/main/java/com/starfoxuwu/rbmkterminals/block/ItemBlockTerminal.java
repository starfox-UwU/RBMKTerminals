package com.starfoxuwu.rbmkterminals.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.compat.hbm.HbmCompat;

/**
 * Item form of the terminals. Besides adding the tooltip it also restricts placement to walls, a terminal floating in
 * mid air or lying on the floor would not make much sense.
 */
public class ItemBlockTerminal extends ItemBlock {

    /** Shown only while HBM is installed, because that is where the screwdriver to use it comes from. */
    public static final String SCREWDRIVER_TOOLTIP = "tooltip.rbmkterminals.screwdriver";

    public ItemBlockTerminal(Block block) {
        super(block);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (side < 2) return false;
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (!(field_150939_a instanceof BlockTerminalBase)) return;

        for (String key : ((BlockTerminalBase) field_150939_a).getTooltipKeys()) {
            if (StatCollector.canTranslate(key)) tooltip.add(StatCollector.translateToLocal(key));
        }

        if (HbmCompat.isAvailable() && StatCollector.canTranslate(SCREWDRIVER_TOOLTIP)) {
            tooltip.add(StatCollector.translateToLocal(SCREWDRIVER_TOOLTIP));
        }
    }
}
