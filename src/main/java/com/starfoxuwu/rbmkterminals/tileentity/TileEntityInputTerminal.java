package com.starfoxuwu.rbmkterminals.tileentity;

import net.minecraft.nbt.NBTTagCompound;

import com.starfoxuwu.rbmkterminals.Config;

/**
 * The terminal that both shows and takes input: right click it to get a keyboard, every line that is submitted is
 * echoed onto the in world screen along with the answer of the command.
 * <p>
 * Row 0 of the screen belongs to the line currently being typed, rows 1 to 17 are the scrolling log.
 */
public class TileEntityInputTerminal extends TileEntityTerminalBase {

    private static final int SIGNAL_MAX = 15;

    /** A line that arrived from the network thread and still has to run on the server thread. */
    private volatile String pendingCommand;

    private int signal;

    @Override
    public int getLogStart() {
        return 1;
    }

    /** Runs one line of the terminal's command language. Server side only. */
    public void eval(String command) {
        processor.eval(command);
    }

    /**
     * Hands a line over to the server thread. Packets are handled on the netty thread, everything a command may touch
     * (blocks, explosions, neighbor updates) belongs to the server thread only, so the line waits here for the next
     * tick.
     */
    public void queueCommand(String command) {
        this.pendingCommand = command;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (worldObj == null || worldObj.isRemote) return;

        String command = pendingCommand;
        if (command == null) return;

        pendingCommand = null;
        eval(command);
    }

    @Override
    public void submit(String command) {
        eval(command);
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    /** @return the analog redstone output of this terminal, 0 - 15 */
    public int getSignalStrength() {
        return signal;
    }

    @Override
    public int getSignal() {
        return signal;
    }

    @Override
    public void setSignal(int newSignal) {
        int clamped = Math.max(0, Math.min(SIGNAL_MAX, newSignal));
        if (clamped == signal) return;

        signal = clamped;
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public boolean isSelfDestructEnabled() {
        return Config.enableTerminalSelfDestruct;
    }

    @Override
    public void selfDestruct() {
        if (worldObj == null || worldObj.isRemote) return;
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        worldObj.createExplosion(null, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 4.0F, false);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        signal = nbt.getByte("signal") & SIGNAL_MAX;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("signal", (byte) signal);
    }
}
