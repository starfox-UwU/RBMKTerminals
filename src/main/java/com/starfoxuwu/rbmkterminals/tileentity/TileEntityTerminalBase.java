package com.starfoxuwu.rbmkterminals.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.starfoxuwu.rbmkterminals.ror.IRORBus;
import com.starfoxuwu.rbmkterminals.ror.IRORInteractive;
import com.starfoxuwu.rbmkterminals.ror.RORBus;
import com.starfoxuwu.rbmkterminals.ror.RORSignal;
import com.starfoxuwu.rbmkterminals.terminal.ITerminalHost;
import com.starfoxuwu.rbmkterminals.terminal.TerminalCommandProcessor;
import com.starfoxuwu.rbmkterminals.terminal.TerminalConfig;

/**
 * Common ground of both terminals: a screen made of 18 text rows that is mirrored to every client watching the block,
 * plus the Redstone-over-Radio side of the house.
 * <p>
 * The terminal is tuned to a channel and then sends and listens on every bus in {@link RORBus} - its own and, with HBM
 * installed, HBM's, which is the one {@code tile.radio_autocal} sends on. What arrives is handed to the command
 * processor, see {@code TerminalCommandProcessor#receiveSignal}.
 * <p>
 * The content is synced with the tile entity description packet, which is both what the server sends when a player
 * starts watching the chunk and what {@code markBlockForUpdate} pushes out after a change.
 */
public abstract class TileEntityTerminalBase extends TileEntity implements ITerminalHost, IRORInteractive {

    public static final int LINE_COUNT = ITerminalHost.LINE_COUNT;

    /** Hard limit for a single row, the renderer truncates whatever does not fit on the screen. */
    public static final int MAX_LINE_LENGTH = 60;

    /** Row 0 is the top most row of the screen. */
    private final String[] lines = new String[LINE_COUNT];

    protected final TerminalCommandProcessor processor = new TerminalCommandProcessor(this);

    private String channel = "";
    private String repeatCommand = "";

    /** The last signal this terminal acted on, so that repeats and world reloads do not run it twice. */
    private String lastSignal = "";

    /**
     * The last signal this terminal published itself and the tick it did so in. Only needed on a bus that does not
     * remember senders, see {@link #isOwnTraffic}.
     */
    private String ownSignal = "";
    private long ownSignalTick = Long.MIN_VALUE;

    /** Colour the screen paints its text in, or {@link TerminalConfig#NO_COLOR} for the default of this type. */
    private int textColor = TerminalConfig.NO_COLOR;

    /** Whether this terminal acts on what other terminals publish on its channel; the screwdriver toggles it. */
    private boolean listening = true;

    protected TileEntityTerminalBase() {
        clearLines();
    }

    /** @return the first row the scrolling log writes to; the input terminal keeps row 0 for the text being typed */
    public int getLogStart() {
        return 0;
    }

    public String[] getLines() {
        return lines;
    }

    public String getLine(int row) {
        return row >= 0 && row < LINE_COUNT ? lines[row] : "";
    }

    public void setLine(int row, String text) {
        if (row >= 0 && row < LINE_COUNT) lines[row] = clamp(text);
    }

    public void clearLines() {
        for (int i = 0; i < LINE_COUNT; i++) {
            lines[i] = "";
        }
    }

    /** Adds a line to the top of the scrolling log, pushing every other line one row down. */
    public void pushLine(String text) {
        int start = getLogStart();
        for (int i = LINE_COUNT - 1; i > start; i--) {
            lines[i] = lines[i - 1];
        }
        lines[start] = clamp(text);
    }

    /** Tells every client watching this block that the screen changed. */
    public void markChanged() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || channel.isEmpty()) return;

        if (!repeatCommand.isEmpty()) {
            publish(repeatCommand);
        }

        // a terminal that was switched to "listen off" still repeats, it just ignores the traffic around it
        if (listening) listen();
    }

    /** Publishes a signal on this terminal's channel, on every bus the terminals are part of. Server side only. */
    private void publish(String signal) {
        if (worldObj == null || worldObj.isRemote || channel.isEmpty() || signal == null || signal.isEmpty()) return;

        ownSignal = signal;
        ownSignalTick = worldObj.getTotalWorldTime();
        RORBus.broadcast(worldObj, channel, signal, xCoord, yCoord, zCoord);
    }

    /** Acts on the freshest signal of every bus this terminal listens to, unless it came from here. */
    private void listen() {
        long now = worldObj.getTotalWorldTime();

        for (IRORBus bus : RORBus.all()) {
            RORSignal entry = bus.listen(worldObj, channel);
            if (entry == null || entry.signal.isEmpty()) continue;
            // only act on fresh traffic, tuning in must not replay whatever happened on that channel ages ago
            if (!bus.isFresh(entry, now)) continue;
            if (isOwnTraffic(entry)) continue;
            if (entry.signal.equals(lastSignal)) continue;

            lastSignal = entry.signal;
            processor.receiveSignal(entry.signal);
            markChanged();
        }
    }

    /**
     * Whether that signal is this terminal's own broadcast.
     * <p>
     * The mod's own bus tells who sent a signal, so there the sender is enough. HBM's does not - its channel only ever
     * holds the last signal - so on it the same text coming back counts as our own as long as it is still the
     * publication we made. HBM stamps a signal in the PRE phase of the following tick, so what a terminal reads is the
     * tick it published in or, when it repeats, the tick before it; the one tick of slack on top keeps that working
     * should the stamping ever shift. Somebody else sending exactly the same text is then only held back for those one
     * or two ticks, afterwards the signal is news again.
     */
    private boolean isOwnTraffic(RORSignal entry) {
        if (entry.isFrom(xCoord, yCoord, zCoord)) return true;

        return !entry.hasSender() && entry.signal.equals(ownSignal) && entry.timeStamp <= ownSignalTick + 1L;
    }

    /** @return true while this terminal keeps repeating a signal on its channel */
    public boolean isRepeating() {
        return !repeatCommand.isEmpty();
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void setChannel(String newChannel) {
        this.channel = clampChannel(newChannel);
        this.lastSignal = ""; // a fresh channel starts with a clean slate
        this.ownSignal = "";
        this.ownSignalTick = Long.MIN_VALUE;
        markChanged();
    }

    @Override
    public String getRepeatCommand() {
        return repeatCommand;
    }

    @Override
    public void setRepeatCommand(String command) {
        this.repeatCommand = clamp(command);
    }

    /** @return the colour this terminal was given, or {@link TerminalConfig#NO_COLOR} if it still wears its default */
    public int getTextColor() {
        return textColor;
    }

    public boolean hasTextColor() {
        return TerminalConfig.hasColor(textColor);
    }

    /** Gives the screen a colour of its own, {@link TerminalConfig#NO_COLOR} puts the default back. */
    public void setTextColor(int color) {
        this.textColor = TerminalConfig.hasColor(color) ? TerminalConfig.clampColor(color) : TerminalConfig.NO_COLOR;
        markChanged();
    }

    /** @return true while this terminal reacts to what is published on its channel */
    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listen) {
        if (this.listening == listen) return;

        this.listening = listen;
        markChanged();
    }

    @Override
    public void broadcastSignal(String signal) {
        if (worldObj == null || worldObj.isRemote) return;
        publish(signal);
    }

    @Override
    public String query(String key) {
        if (worldObj == null) return "";
        if ("pos".equals(key)) return xCoord + " / " + yCoord + " / " + zCoord;
        if ("dim".equals(key)) return String.valueOf(worldObj.provider.dimensionId);
        if ("players".equals(key)) return String.valueOf(worldObj.playerEntities.size());
        if ("time".equals(key)) {
            long ticks = worldObj.getWorldTime();
            long dayTime = ticks % 24000L;
            return "day " + ticks / 24000L
                + ", "
                + pad((dayTime / 1000L + 6L) % 24L)
                + ":"
                + pad(dayTime % 1000L * 60L / 1000L);
        }
        return "";
    }

    @Override
    public String[] getFunctionInfo() {
        return TerminalCommandProcessor.getFunctionInfo(isInteractive());
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        return processor.runRORFunction(name, params);
    }

    // the display terminal can do none of this, the input terminal overrides all of it

    @Override
    public int getSignal() {
        return 0;
    }

    @Override
    public void setSignal(int signal) {}

    @Override
    public void submit(String command) {}

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean isSelfDestructEnabled() {
        return false;
    }

    @Override
    public void selfDestruct() {}

    private static String pad(long value) {
        return value < 10L ? "0" + value : String.valueOf(value);
    }

    private static String clamp(String text) {
        if (text == null) return "";
        String flat = text.replace('\n', ' ')
            .replace('\r', ' ');
        return flat.length() > MAX_LINE_LENGTH ? flat.substring(0, MAX_LINE_LENGTH) : flat;
    }

    private static String clampChannel(String name) {
        if (name == null) return "";
        String flat = name.trim()
            .replace('\n', ' ')
            .replace('\r', ' ');
        return flat.length() > TerminalCommandProcessor.MAX_CHANNEL_LENGTH
            ? flat.substring(0, TerminalCommandProcessor.MAX_CHANNEL_LENGTH)
            : flat;
    }

    /**
     * Whether this player is allowed to reconfigure the terminal, mirroring
     * {@code TileEntityRBMKIndicator#hasPermission}: you have to stand next to the block you are editing.
     */
    public boolean hasPermission(EntityPlayer player) {
        return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) < 15D * 15D;
    }

    /**
     * Applies the configuration the screwdriver window sent over, the counterpart of
     * {@code TileEntityRBMKIndicator#receiveControl}. Only called on the server, which then pushes the new state to
     * every client watching the block.
     */
    public void receiveControl(NBTTagCompound data) {
        setChannel(data.getString(TerminalConfig.KEY_CHANNEL));
        setTextColor(
            data.hasKey(TerminalConfig.KEY_COLOR) ? data.getInteger(TerminalConfig.KEY_COLOR)
                : TerminalConfig.NO_COLOR);
        setListening(!data.hasKey(TerminalConfig.KEY_LISTENING) || data.getBoolean(TerminalConfig.KEY_LISTENING));
        markChanged();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        readScreenNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        writeScreenNBT(nbt);
    }

    public void readScreenNBT(NBTTagCompound nbt) {
        for (int i = 0; i < LINE_COUNT; i++) {
            lines[i] = clamp(nbt.getString("line" + i));
        }
        channel = clampChannel(nbt.getString(TerminalConfig.KEY_CHANNEL));
        repeatCommand = clamp(nbt.getString("repeat"));
        lastSignal = clamp(nbt.getString("lastSignal"));
        textColor = nbt.hasKey(TerminalConfig.KEY_COLOR)
            ? TerminalConfig.clampColor(nbt.getInteger(TerminalConfig.KEY_COLOR))
            : TerminalConfig.NO_COLOR;
        listening = !nbt.hasKey(TerminalConfig.KEY_LISTENING) || nbt.getBoolean(TerminalConfig.KEY_LISTENING);
    }

    public void writeScreenNBT(NBTTagCompound nbt) {
        for (int i = 0; i < LINE_COUNT; i++) {
            nbt.setString("line" + i, lines[i] == null ? "" : lines[i]);
        }
        nbt.setString(TerminalConfig.KEY_CHANNEL, channel);
        nbt.setString("repeat", repeatCommand);
        nbt.setString("lastSignal", lastSignal);
        if (hasTextColor()) nbt.setInteger(TerminalConfig.KEY_COLOR, textColor);
        nbt.setBoolean(TerminalConfig.KEY_LISTENING, listening);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeScreenNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readScreenNBT(packet.func_148857_g());
    }
}
