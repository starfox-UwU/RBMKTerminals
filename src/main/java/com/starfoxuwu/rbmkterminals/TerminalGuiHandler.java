package com.starfoxuwu.rbmkterminals;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.starfoxuwu.rbmkterminals.network.ModNetwork;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.common.network.IGuiHandler;

/**
 * Routes the terminal GUI. The client side window is created through the proxy so that this class stays free of any
 * client only types.
 */
public class TerminalGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null; // the terminal is not an inventory, the screen lives in the world
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ModNetwork.GUI_INPUT_TERMINAL) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEntityInputTerminal)) return null;

            return RBMKTerminals.proxy.getTerminalGui((TileEntityInputTerminal) tile);
        }

        if (id == ModNetwork.GUI_TERMINAL_CONFIG) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEntityTerminalBase)) return null;

            return RBMKTerminals.proxy.getTerminalConfigGui((TileEntityTerminalBase) tile);
        }

        return null;
    }
}
