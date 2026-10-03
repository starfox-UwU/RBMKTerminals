package com.starfoxuwu.rbmkterminals.tileentity;

/**
 * The display only terminal.
 * <p>
 * It has no GUI and no keyboard: lines can only be pushed onto it from the outside, either with the
 * {@code /terminal write} command or by another mod calling {@link #pushLine(String)}.
 */
public class TileEntityDisplayTerminal extends TileEntityTerminalBase {
}
