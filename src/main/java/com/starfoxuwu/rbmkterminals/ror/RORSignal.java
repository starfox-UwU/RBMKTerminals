package com.starfoxuwu.rbmkterminals.ror;

/**
 * One signal as it was read off a bus: the text that was published, the world tick it was published in and, if the bus
 * knows about it, where it came from.
 * <p>
 * The mod's own bus remembers the sender, HBM's does not, which is why a signal can be read with or without that
 * information and why {@link #isFrom} only answers for the former.
 */
public final class RORSignal {

    /** The signal as it goes over the air, e.g. {@code write!hello radio}. */
    public final String signal;

    /** Total world time of the tick the signal was published in, or -1 if the bus does not stamp its signals. */
    public final long timeStamp;

    /** Whether the bus this signal came from remembers who published it. */
    private final boolean hasSender;

    private final int senderX;
    private final int senderY;
    private final int senderZ;

    private RORSignal(String signal, long timeStamp, boolean hasSender, int senderX, int senderY, int senderZ) {
        this.signal = signal;
        this.timeStamp = timeStamp;
        this.hasSender = hasSender;
        this.senderX = senderX;
        this.senderY = senderY;
        this.senderZ = senderZ;
    }

    /** A signal from a bus that remembers who published it. */
    public static RORSignal fromSender(String signal, long timeStamp, int x, int y, int z) {
        return new RORSignal(signal, timeStamp, true, x, y, z);
    }

    /**
     * A signal from a bus that does not remember who published it, e.g. HBM's {@code RTTYSystem}, whose channel only
     * holds the signal itself. A terminal has to recognise its own traffic by content on such a bus.
     */
    public static RORSignal withoutSender(String signal, long timeStamp) {
        return new RORSignal(signal, timeStamp, false, 0, 0, 0);
    }

    /** @return true if this very block published the signal, never true for a bus that hides the sender */
    public boolean isFrom(int x, int y, int z) {
        return hasSender && senderX == x && senderY == y && senderZ == z;
    }

    public boolean hasSender() {
        return hasSender;
    }
}
