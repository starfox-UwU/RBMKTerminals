package com.starfoxuwu.rbmkterminals.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.tileentity.TileEntityDisplayTerminal;

/**
 * The display only terminal: it shows whatever is pushed onto it and does nothing else, right clicking it is
 * deliberately a no-op.
 */
public class BlockDisplayTerminal extends BlockTerminalBase {

    public static final String[] TOOLTIP_KEYS = new String[] { "tooltip.rbmkterminals.display_terminal.1",
        "tooltip.rbmkterminals.display_terminal.2", "tooltip.rbmkterminals.display_terminal.3" };

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityDisplayTerminal();
    }

    @Override
    public String[] getTooltipKeys() {
        return TOOLTIP_KEYS;
    }
}
