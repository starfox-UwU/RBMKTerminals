package com.starfoxuwu.rbmkterminals.terminal;

/**
 * The rules of the screwdriver configuration, kept free of any Minecraft type for the same reason
 * {@link ITerminalHost} is: the interesting parts can then be tested without launching the game.
 * <p>
 * The fields mirror the ones HBM's {@code tile.rbmk_indicator} offers for each of its six lights - a channel to read
 * from, a colour and whether the device reacts to what arrives - only that a terminal has a single screen instead of
 * six units.
 */
public final class TerminalConfig {

    /** NBT keys, shared by the tile entity's own tags and the configuration packet. */
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_COLOR = "color";
    public static final String KEY_LISTENING = "listening";

    /** A terminal that was never given a colour of its own paints with the default of its type. */
    public static final int NO_COLOR = -1;

    /** Number of hex digits in a colour. */
    public static final int COLOR_DIGITS = 6;

    /** What an input terminal's screen looks like before anyone configures it. */
    public static final int DEFAULT_INPUT_COLOR = 0x00FF00;

    /** What a display terminal's screen looks like before anyone configures it. */
    public static final int DEFAULT_DISPLAY_COLOR = 0xFFB000;

    /** The palette the configuration window offers; the first two entries are the two defaults. */
    public static final int[] COLOR_PRESETS = { DEFAULT_INPUT_COLOR, DEFAULT_DISPLAY_COLOR, 0x00E0FF, 0xFF4040,
        0xFFFFFF, 0x808080 };

    private TerminalConfig() {}

    /** @return the colour a terminal of this kind paints its text in while it has no colour of its own */
    public static int defaultColor(boolean interactive) {
        return interactive ? DEFAULT_INPUT_COLOR : DEFAULT_DISPLAY_COLOR;
    }

    /** @return true if the given value is a colour and not the "no colour set" marker */
    public static boolean hasColor(int color) {
        return color != NO_COLOR;
    }

    /** @return the colour, forced into the RGB range */
    public static int clampColor(int color) {
        if (color < 0x000000) return 0x000000;
        return color > 0xFFFFFF ? 0xFFFFFF : color;
    }

    /** @return the colour as six hex digits, without a prefix */
    public static String formatColor(int color) {
        String hex = Integer.toHexString(clampColor(color));
        StringBuilder builder = new StringBuilder();
        for (int i = hex.length(); i < COLOR_DIGITS; i++) {
            builder.append('0');
        }
        return builder.append(hex)
            .toString();
    }

    /**
     * Reads a colour out of a text field.
     *
     * @param text six hex digits, with or without a leading {@code #}
     * @return the packed RGB value, or {@link #NO_COLOR} if the text is not a colour (an empty field included, which is
     *         how the window resets a terminal back to its default look)
     */
    public static int parseColor(String text) {
        if (text == null) return NO_COLOR;

        String trimmed = text.trim();
        if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        if (trimmed.length() != COLOR_DIGITS) return NO_COLOR;

        int value = 0;
        for (int i = 0; i < COLOR_DIGITS; i++) {
            int digit = Character.digit(trimmed.charAt(i), 16);
            if (digit < 0) return NO_COLOR;
            value = value << 4 | digit;
        }
        return value;
    }
}
