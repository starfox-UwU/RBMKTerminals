package com.starfoxuwu.rbmkterminals.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.RBMKTerminals;
import com.starfoxuwu.rbmkterminals.network.ModNetwork;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;

/**
 * The terminal that takes input: right clicking opens a keyboard, and the line the player sends drives both the screen
 * and, through the {@code rs} command, the redstone output of the block.
 */
public class BlockInputTerminal extends BlockTerminalBase {

    public static final String[] TOOLTIP_KEYS = new String[] { "tooltip.rbmkterminals.input_terminal.1",
        "tooltip.rbmkterminals.input_terminal.2", "tooltip.rbmkterminals.input_terminal.3",
        "tooltip.rbmkterminals.input_terminal.4" };

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityInputTerminal();
    }

    @Override
    protected boolean onActivated(World world, EntityPlayer player, int x, int y, int z, int side, float hitX,
        float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        // the terminal is not an inventory, so the keyboard is opened on the client directly; everything that is
        // typed is validated by the server before a single command runs
        if (world.isRemote) player.openGui(RBMKTerminals.instance, ModNetwork.GUI_INPUT_TERMINAL, world, x, y, z);
        return true;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return getSignalStrength(world, x, y, z);
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return getSignalStrength(world, x, y, z);
    }

    private static int getSignalStrength(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntityInputTerminal ? ((TileEntityInputTerminal) tile).getSignalStrength() : 0;
    }

    @Override
    public String[] getTooltipKeys() {
        return TOOLTIP_KEYS;
    }
}
