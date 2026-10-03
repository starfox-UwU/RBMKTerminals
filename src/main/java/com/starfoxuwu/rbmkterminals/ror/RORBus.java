package com.starfoxuwu.rbmkterminals.ror;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;

/**
 * Every bus both terminals send on and listen to.
 * <p>
 * The list always holds the mod's own {@link RTTYSystem} first; {@link #install} adds more on top, today only HBM's
 * bus, without which a terminal would never see what {@code tile.radio_autocal} sends. Installing happens once, during
 * preInit, and no bus is ever removed again - a terminal may therefore walk the list while the server thread ticks.
 */
public final class RORBus {

    private static final List<IRORBus> BUSES = new ArrayList<IRORBus>();

    static {
        BUSES.add(new Local());
    }

    private RORBus() {}

    /** @return every bus a terminal takes part in, the mod's own one being the first */
    public static List<IRORBus> all() {
        return Collections.unmodifiableList(BUSES);
    }

    /** Adds a bus, e.g. the adapter for HBM's radio traffic. */
    public static void install(IRORBus bus) {
        if (bus == null || BUSES.contains(bus)) return;

        BUSES.add(bus);
    }

    /** Publishes one signal on all buses at once. */
    public static void broadcast(World world, String channel, String signal, int x, int y, int z) {
        for (IRORBus bus : BUSES) {
            bus.broadcast(world, channel, signal, x, y, z);
        }
    }

    /**
     * The mod's own bus: a plain {@link RTTYSystem} table, which remembers the sender and counts a signal as news for
     * the tick it was published in.
     */
    private static final class Local implements IRORBus {

        @Override
        public void broadcast(World world, String channel, String signal, int x, int y, int z) {
            RTTYSystem.broadcast(world, channel, signal, x, y, z);
        }

        @Override
        public RORSignal listen(World world, String channel) {
            RTTYSystem.Channel entry = RTTYSystem.listen(world, channel);
            if (entry == null) return null;

            return RORSignal.fromSender(entry.signal, entry.timeStamp, entry.senderX, entry.senderY, entry.senderZ);
        }

        @Override
        public boolean isFresh(RORSignal signal, long now) {
            return signal.timeStamp >= now - 1L;
        }
    }
}
