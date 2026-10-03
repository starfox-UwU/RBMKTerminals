package com.starfoxuwu.rbmkterminals.ror;

import net.minecraft.world.World;

/**
 * One Redstone-over-Radio bus the terminals take part in.
 * <p>
 * A bus is a {@code (world, channel) -> last signal} table: senders publish, receivers poll every tick and decide for
 * themselves whether what they read is news. The mod brings its own bus ({@link RORBus} registers it), HBM has one of
 * its own, and with HBM installed the terminals join both, which is how a signal from {@code tile.radio_autocal}
 * reaches a terminal at all.
 * <p>
 * Freshness is part of the bus because the two count their ticks differently: a signal stays "current" for one tick on
 * this mod's bus, for two on HBM's, whose timestamp is written a phase earlier.
 */
public interface IRORBus {

    /** Publishes a signal on a channel, replacing whatever was broadcast before. Server side only. */
    void broadcast(World world, String channel, String signal, int x, int y, int z);

    /**
     * @return the last signal published on that channel, or null if there never was one; whether it is still news is
     *         what {@link #isFresh} answers, not this method
     */
    RORSignal listen(World world, String channel);

    /** @return true if that signal is recent enough to be acted on in a tick that sees the given world time */
    boolean isFresh(RORSignal signal, long now);
}
