package com.starfoxuwu.rbmkterminals.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Carries one line the player typed in the terminal GUI to the server. */
public class PacketTerminalCommand implements IMessage {

    private static final double MAX_DISTANCE_SQ = 15D * 15D;

    private int x;
    private int y;
    private int z;
    private String command = "";

    public PacketTerminalCommand() {}

    public PacketTerminalCommand(int x, int y, int z, String command) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.command = command;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        command = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        ByteBufUtils.writeUTF8String(buf, command == null ? "" : command);
    }

    public static class Handler implements IMessageHandler<PacketTerminalCommand, IMessage> {

        @Override
        public IMessage onMessage(PacketTerminalCommand message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || player.worldObj == null) return null;

            // never trust a client, not even about where it is standing
            if (player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D) > MAX_DISTANCE_SQ)
                return null;

            TileEntity tile = player.worldObj.getTileEntity(message.x, message.y, message.z);
            if (tile instanceof TileEntityInputTerminal) {
                ((TileEntityInputTerminal) tile).queueCommand(message.command);
            }
            return null;
        }
    }
}
