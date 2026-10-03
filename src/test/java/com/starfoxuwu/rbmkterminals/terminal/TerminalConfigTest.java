package com.starfoxuwu.rbmkterminals.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The colour field of the screwdriver window is plain logic, so it is covered without a running game, the same way the
 * command language is.
 */
public class TerminalConfigTest {

    @Test
    public void coloursAreWrittenWithSixDigits() {
        assertEquals("00ff00", TerminalConfig.formatColor(0x00FF00));
        assertEquals("000abc", TerminalConfig.formatColor(0x000ABC));
        assertEquals("000000", TerminalConfig.formatColor(0x000000));
        assertEquals("ffffff", TerminalConfig.formatColor(0xFFFFFF));
    }

    @Test
    public void coloursSurviveARoundTrip() {
        int[] colours = { 0x000000, 0x00FF00, 0xFFB000, 0x123456, 0xFFFFFF };
        for (int colour : colours) {
            assertEquals(colour, TerminalConfig.parseColor(TerminalConfig.formatColor(colour)));
        }
    }

    @Test
    public void parseAcceptsAHashAndAnyCase() {
        assertEquals(0x00FF00, TerminalConfig.parseColor("00ff00"));
        assertEquals(0x00FF00, TerminalConfig.parseColor("#00FF00"));
        assertEquals(0x00FF00, TerminalConfig.parseColor("  #00ff00  "));
    }

    @Test
    public void parseRejectsWhatIsNotAColour() {
        // an empty field is how the window asks for the terminal's own default, so it must not pass as a colour
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor(""));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor("   "));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor(null));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor("fff"));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor("00ff00ff"));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor("00ff0g"));
        assertEquals(TerminalConfig.NO_COLOR, TerminalConfig.parseColor("#"));
    }

    @Test
    public void blackIsAColourAndNotTheDefaultMarker() {
        assertEquals(0x000000, TerminalConfig.parseColor("000000"));
        assertTrue(TerminalConfig.hasColor(0x000000));
        assertFalse(TerminalConfig.hasColor(TerminalConfig.NO_COLOR));
    }

    @Test
    public void coloursAreClampedToTheRgbRange() {
        assertEquals(0x000000, TerminalConfig.clampColor(-1));
        assertEquals(0xFFFFFF, TerminalConfig.clampColor(Integer.MAX_VALUE));
        assertEquals(0x123456, TerminalConfig.clampColor(0x123456));
    }

    @Test
    public void everyTerminalTypeHasItsOwnDefault() {
        assertEquals(0x00FF00, TerminalConfig.defaultColor(true));
        assertEquals(0xFFB000, TerminalConfig.defaultColor(false));
    }

    @Test
    public void thePaletteOffersBothDefaults() {
        assertTrue(TerminalConfig.COLOR_PRESETS.length > 0);
        assertEquals(TerminalConfig.DEFAULT_INPUT_COLOR, TerminalConfig.COLOR_PRESETS[0]);
        assertEquals(TerminalConfig.DEFAULT_DISPLAY_COLOR, TerminalConfig.COLOR_PRESETS[1]);
    }
}
