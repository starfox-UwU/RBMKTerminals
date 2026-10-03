package com.starfoxuwu.rbmkterminals.ror;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.World;

/**
 * The Redstone-over-Radio bus, a slimmed down version of HBM's {@code RTTYSystem}.
 * <p>
 * A channel simply remembers the last signal that was published on it, along with the tick it was published in. There
 * is no listener registry: senders write and receivers poll every tick, which is what HBM's radio machines do as well.
 * <p>
 * Everything here happens on the server thread, a world is only weakly referenced so that unloading it does not leave
 * the bus behind.
 */
public final class RTTYSystem {

    private static final Map<World, Map<String, Channel>> CHANNELS = new WeakHashMap<World, Map<String, Channel>>();

    private RTTYSystem() {}

    /** Publishes a signal on a channel, replacing whatever was broadcast before. Server side only. */
    public static void broadcast(World world, String channel, String signal, int x, int y, int z) {
        if (world == null || channel == null || channel.isEmpty() || signal == null || signal.isEmpty()) return;

        Map<String, Channel> channels = CHANNELS.get(world);
        if (channels == null) {
            channels = new HashMap<String, Channel>();
            CHANNELS.put(world, channels);
        }

        Channel entry = new Channel();
        entry.timeStamp = world.getTotalWorldTime();
        entry.signal = signal;
        entry.senderX = x;
        entry.senderY = y;
        entry.senderZ = z;

        channels.put(channel, entry);
    }

    /** @return the last signal published on that channel, or null if there never was one */
    public static Channel listen(World world, String channel) {
        if (world == null || channel == null || channel.isEmpty()) return null;

        Map<String, Channel> channels = CHANNELS.get(world);
        return channels == null ? null : channels.get(channel);
    }

    /** Forgets everything a world has published. */
    public static void clear(World world) {
        CHANNELS.remove(world);
    }

    public static class Channel {

        /** The total world time the signal was published in. */
        public long timeStamp;
        public String signal = "";

        /** Where the signal came from, so that a sender does not answer itself. */
        public int senderX;
        public int senderY;
        public int senderZ;

        public boolean isFrom(int x, int y, int z) {
            return senderX == x && senderY == y && senderZ == z;
        }
    }
}
