package com.starfoxuwu.rbmkterminals.compat.hbm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import net.minecraft.world.World;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.starfoxuwu.rbmkterminals.ror.RORSignal;

/**
 * Checks the seam between this mod and HBM's radio network: that a signal {@code tile.radio_autocal} sends ends up
 * readable for a terminal through {@link HbmRORBus}, and that the bridge treats it the way HBM's own devices do.
 * <p>
 * HBM is an optional dependency, so every HBM type is looked up by name and the test steps aside instead of failing
 * when the mod is not on the classpath. Nothing outside the game can build a {@code World}, so the bus is read with a
 * {@code null} one - HBM only ever uses it as a map key - and the signal is put into the readable map the way
 * {@code RTTYSystem#updateBroadcastQueue} would, since that step needs the running server.
 */
public class HbmRORBusTest {

    private static final String RTTY = "com.hbm.tileentity.network.RTTYSystem";
    private static final String CHANNEL = "com.hbm.tileentity.network.RTTYSystem$RTTYChannel";
    private static final String BUS = "com.starfoxuwu.rbmkterminals.compat.hbm.HbmRORBus";

    /** A world time of our own, since no world is around to hand one out. */
    private static final long PUBLISHED = 40L;

    private Class<?> rtty;
    private Class<?> channel;
    private Object bus;

    /** {@code RTTYSystem#broadcast}, the map receivers read. */
    private Map<Object, Object> published;
    /** {@code RTTYSystem#newMessages}, the queue senders write into. */
    private Map<Object, Object> queued;

    @Before
    public void setUp() throws Exception {
        rtty = load(RTTY);
        channel = load(CHANNEL);

        published = map("broadcast");
        queued = map("newMessages");
        published.clear();
        queued.clear();

        bus = load(BUS).getDeclaredConstructor()
            .newInstance();
    }

    @After
    public void tearDown() {
        if (published != null) published.clear();
        if (queued != null) queued.clear();
    }

    /**
     * The whole point of the bridge: the MSES1 {@code send} statement of an AUTOCAL puts its buffer onto HBM's bus,
     * and that is what a terminal reads.
     */
    @Test
    public void anAutocalSignalIsWhatTheTerminalReads() throws Exception {
        send("control", "write!reactor outlet 1200K");
        flush(PUBLISHED);

        RORSignal signal = listen("control");

        assertNotNull("the signal did not reach the terminals", signal);
        assertEquals("write!reactor outlet 1200K", signal.signal);
        assertEquals(PUBLISHED, signal.timeStamp);
    }

    /** A channel nobody ever published on is empty, tuning a terminal to it must not invent a signal. */
    @Test
    public void anEmptyChannelReadsAsNothing() throws Exception {
        assertNull(listen("control"));
    }

    /**
     * HBM stamps a signal a tick behind its receivers, see {@code TileEntityRadioTorchReceiver}, so the bridge has to
     * offer the same two tick window - otherwise a terminal would either miss traffic or act on stale signals.
     */
    @Test
    public void theFreshnessWindowIsTheOneHbmUses() throws Exception {
        send("control", "7");
        flush(PUBLISHED);
        RORSignal signal = listen("control");

        assertTrue("the tick after publication", fresh(signal, PUBLISHED + 1L));
        assertFalse("two ticks later it is history", fresh(signal, PUBLISHED + 2L));
    }

    /** A channel that was never published on wears the -1 timestamp HBM writes into a fresh record. */
    @Test
    public void anUnstampedSignalIsNeverFresh() throws Exception {
        send("control", "7");
        flush(-1L);

        assertFalse(fresh(listen("control"), PUBLISHED));
    }

    /** HBM's channel only holds the signal itself, which is why the bridge reports it as anonymous traffic. */
    @Test
    public void theBridgeKeepsNoSender() throws Exception {
        send("control", "clear");
        flush(PUBLISHED);

        RORSignal signal = listen("control");

        assertFalse(signal.hasSender());
        assertFalse(signal.isFrom(0, 0, 0));
    }

    /**
     * HBM's maps are static and never cleaned up, so the bridge refuses to publish without a world instead of leaving
     * an entry behind that no world owns.
     */
    @Test
    public void withoutAWorldNothingIsPublished() throws Exception {
        Method broadcast = bus.getClass()
            .getMethod("broadcast", World.class, String.class, String.class, int.class, int.class, int.class);

        broadcast.invoke(bus, null, "control", "write!hello", 0, 0, 0);

        assertTrue(queued.isEmpty());
    }

    /** Runs the MSES1 {@code send} statement: HBM's own broadcast into the queue that is handed over a tick later. */
    private void send(String channelName, String signal) throws Exception {
        Method broadcast = rtty.getMethod("broadcast", World.class, String.class, Object.class);
        broadcast.invoke(null, null, channelName, signal);
    }

    /** The part of {@code RTTYSystem#updateBroadcastQueue} that does not need the running server. */
    private void flush(long timeStamp) throws Exception {
        for (Object element : queued.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;

            Object record = channel.getDeclaredConstructor()
                .newInstance();
            channel.getField("timeStamp")
                .setLong(record, timeStamp);
            channel.getField("signal")
                .set(record, entry.getValue());

            published.put(entry.getKey(), record);
        }

        queued.clear();
    }

    private RORSignal listen(String channelName) throws Exception {
        Method listen = bus.getClass()
            .getMethod("listen", World.class, String.class);
        return (RORSignal) listen.invoke(bus, null, channelName);
    }

    private boolean fresh(RORSignal signal, long now) throws Exception {
        Method isFresh = bus.getClass()
            .getMethod("isFresh", RORSignal.class, long.class);
        return (Boolean) isFresh.invoke(bus, signal, now);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> map(String name) throws Exception {
        Field field = rtty.getField(name);
        return (Map<Object, Object>) field.get(null);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            Assume.assumeNoException("HBM is not installed", ex);
            throw new AssertionError(ex); // assumeNoException always throws, this only keeps the compiler happy
        }
    }
}
