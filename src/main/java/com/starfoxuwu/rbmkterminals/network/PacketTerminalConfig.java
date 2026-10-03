package com.starfoxuwu.rbmkterminals.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Carries the screwdriver window's settings to the server.
 * <p>
 * Same shape as HBM's {@code NBTControlPacket}: the window wraps everything it edited into one tag, the server looks up
 * the block the tag claims to belong to, checks that the player is close enough and hands the tag to the tile entity.
 */
public class PacketTerminalConfig implements IMessage {

    private int x;
    private int y;
    private int z;
    private NBTTagCompound data = new NBTTagCompound();

    public PacketTerminalConfig() {}

    public PacketTerminalConfig(int x, int y, int z, NBTTagCompound data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        ByteBufUtils.writeTag(buf, data == null ? new NBTTagCompound() : data);
    }

    public static class Handler implements IMessageHandler<PacketTerminalConfig, IMessage> {

        @Override
        public IMessage onMessage(PacketTerminalConfig message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || player.worldObj == null || message.data == null) return null;

            // never trust a client, not even about where it is standing
            TileEntity tile = player.worldObj.getTileEntity(message.x, message.y, message.z);
            if (!(tile instanceof TileEntityTerminalBase)) return null;

            TileEntityTerminalBase terminal = (TileEntityTerminalBase) tile;
            if (!terminal.hasPermission(player)) return null;

            terminal.receiveControl(message.data);
            return null;
        }
    }
}
