package com.starfoxuwu.rbmkterminals.ror;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The signal record is what every bus hands to a terminal, and its two flavours are what tells the terminal whether it
 * may look for its own traffic in there: the mod's own bus remembers the sender, HBM's does not.
 */
public class RORSignalTest {

    @Test
    public void aSignalFromTheOwnBusKnowsWhereItCameFrom() {
        RORSignal signal = RORSignal.fromSender("write!hello", 12L, 4, 65, -8);

        assertEquals("write!hello", signal.signal);
        assertEquals(12L, signal.timeStamp);
        assertTrue(signal.hasSender());
        assertTrue(signal.isFrom(4, 65, -8));
        assertFalse(signal.isFrom(4, 65, -7));
    }

    @Test
    public void anAnonymousSignalNeverLooksLikeItCameFromTheTerminalItself() {
        RORSignal signal = RORSignal.withoutSender("write!hello", 12L);

        assertEquals("write!hello", signal.signal);
        assertEquals(12L, signal.timeStamp);
        assertFalse(signal.hasSender());
        assertFalse(signal.isFrom(0, 0, 0));
    }
}
