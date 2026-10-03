package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.block.BlockDisplayTerminal;

import api.hbm.block.IToolable;

/**
 * The display terminal as HBM sees it, the same shape as {@code RBMKIndicator}: a panel that implements
 * {@link IToolable} and answers a screwdriver with its configuration window.
 */
public class BlockHbmDisplayTerminal extends BlockDisplayTerminal implements IToolable {

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY,
        float fZ, ToolType tool) {
        if (!HbmTerminalBlocks.drivesConfiguration(tool)) return false;

        openTerminalConfig(world, player, x, y, z);
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        // the clicked block is asked before the held item is, so a screwdriver has to be taken here as well - going
        // through ItemTooling#onItemUse only would mean the screwdriver works while sneaking and nothing else
        if (HbmTerminalBlocks.holdsScrewdriver(player)) {
            openTerminalConfig(world, player, x, y, z);
            return true;
        }

        return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
    }
}
