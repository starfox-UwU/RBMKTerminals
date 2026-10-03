package com.starfoxuwu.rbmkterminals.terminal;

/**
 * The part of a terminal that the command processor is allowed to touch.
 * <p>
 * This interface is deliberately free of any Minecraft types so that the command language, including the
 * Redstone-over-Radio part, can be tested without launching the game.
 */
public interface ITerminalHost {

    /** Number of rows a terminal screen can display. */
    int LINE_COUNT = 18;

    /** Adds a line to the top of the scrolling log, pushing every other line one row down. */
    void pushLine(String text);

    /** Empties the whole screen. */
    void clearLines();

    /** @return the raw screen content, the first entry being the top most row */
    String[] getLines();

    /** Overwrites a single row, indices start at 0. */
    void setLine(int index, String text);

    /** @return the current analog redstone output, 0 - 15 */
    int getSignal();

    /** Sets the analog redstone output, 0 - 15. */
    void setSignal(int signal);

    /** @return this terminal's ROR channel, empty if it is not listening to anything */
    String getChannel();

    /** Tunes this terminal to a ROR channel. An empty name takes it off the air. */
    void setChannel(String channel);

    /** @return the signal that is repeated on the channel every tick, empty if none */
    String getRepeatCommand();

    /** Sets the signal that should be repeated on the channel every tick. */
    void setRepeatCommand(String command);

    /** Publishes a signal on this terminal's channel, does nothing without a channel. */
    void broadcastSignal(String signal);

    /** Runs a line of input on behalf of a remote terminal, see the {@code submit} ROR function. */
    void submit(String command);

    /** @return true if this terminal has a keyboard and therefore accepts {@code submit} */
    boolean isInteractive();

    /** @return a piece of information about the terminal, e.g. its position or the world time */
    String query(String key);

    /** @return true if the (config gated) self destruct command may be used */
    boolean isSelfDestructEnabled();

    /** Removes this terminal together with a good part of its surroundings. */
    void selfDestruct();

    /** Marks the screen content as changed so that nearby clients receive the new lines. */
    void markChanged();
}
