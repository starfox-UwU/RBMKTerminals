package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.world.World;

import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.starfoxuwu.rbmkterminals.ror.IRORBus;
import com.starfoxuwu.rbmkterminals.ror.RORBus;
import com.starfoxuwu.rbmkterminals.ror.RORSignal;

/**
 * Makes HBM's radio network the one the terminals listen to as well.
 * <p>
 * HBM keeps its {@code RTTYSystem} to itself, so everything {@code tile.radio_autocal} sends with its MSES1
 * {@code send} statement - and everything any other HBM radio machine publishes - stays on a bus the terminals never
 * looked at. This adapter hands that bus to {@link RORBus}, which then reads and writes it alongside the mod's own
 * one: a signal sent from either side is heard on the other.
 * <p>
 * Everything here mentions an HBM type, so the class may only be loaded after {@link HbmCompat} has established that
 * HBM is installed.
 */
public final class HbmRORBus implements IRORBus {

    /** One bridge is enough, and keeping it means installing twice cannot end up in the list twice. */
    private static final HbmRORBus INSTANCE = new HbmRORBus();

    HbmRORBus() {}

    /** Registers the bridge with {@link RORBus}. Called once, during preInit. */
    static void install() {
        RORBus.install(INSTANCE);
    }

    @Override
    public void broadcast(World world, String channel, String signal, int x, int y, int z) {
        if (world == null || channel == null || channel.isEmpty() || signal == null || signal.isEmpty()) return;

        // HBM queues the signal and hands it out one tick later, and it adds up numeric signals published in the same
        // tick - the same treatment every HBM radio machine gets, terminals are not special
        RTTYSystem.broadcast(world, channel, signal);
    }

    @Override
    public RORSignal listen(World world, String channel) {
        RTTYChannel entry = RTTYSystem.listen(world, channel);
        if (entry == null || entry.signal == null) return null;

        // HBM's channel carries an Object on purpose: a number from a gauge, a single character from a telex, an
        // encoded note. HBM itself reads it with "" + signal, so that is what a terminal gets to see as well.
        return RORSignal.withoutSender("" + entry.signal, entry.timeStamp);
    }

    @Override
    public boolean isFresh(RORSignal signal, long now) {
        // RTTYSystem stamps a signal while moving its queue over, in the PRE phase of the server tick, so its
        // timestamp is a tick behind what the receivers see. This is the very window HBM's own devices use, see
        // TileEntityRadioTorchReceiver.
        return signal.timeStamp != -1L && signal.timeStamp > now - 2L;
    }
}
