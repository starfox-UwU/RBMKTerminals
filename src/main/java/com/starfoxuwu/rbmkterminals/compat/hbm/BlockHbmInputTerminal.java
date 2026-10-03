package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.block.BlockInputTerminal;

import api.hbm.block.IToolable;

/**
 * The input terminal as HBM sees it.
 * <p>
 * This one does have a right click of its own, so without the check below the keyboard would swallow every screwdriver
 * click; HBM's own panels never ran into that because none of them opens a GUI on a plain right click.
 */
public class BlockHbmInputTerminal extends BlockInputTerminal implements IToolable {

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
        if (HbmTerminalBlocks.holdsScrewdriver(player)) {
            openTerminalConfig(world, player, x, y, z);
            return true;
        }

        return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
    }
}
