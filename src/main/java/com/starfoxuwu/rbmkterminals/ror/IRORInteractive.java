package com.starfoxuwu.rbmkterminals.ror;

import java.util.Locale;

/**
 * Redstone-over-Radio, the remote control language HBM's machines speak to each other.
 * <p>
 * A signal is a single string of the form {@code name!param:param}. Signals prefixed with {@link #PREFIX_FUNCTION}
 * are calls to {@link #runRORFunction}, signals prefixed with {@link #PREFIX_VALUE} are read outs. This interface is
 * modelled after {@code api.hbm.redstoneoverradio.IRORInteractive} so that both the terminals and devices from other
 * mods can take part in the same network.
 */
public interface IRORInteractive {

    String NAME_SEPARATOR = "!";
    String PARAM_SEPARATOR = ":";

    String EX_NULL = "Exception: Null Command";
    String EX_NAME = "Exception: Multiple Name Separators";
    String EX_FORMAT = "Exception: Parameter in Invalid Format";

    String PREFIX_VALUE = "VAL:";
    String PREFIX_FUNCTION = "FUN:";

    /** @return the functions this device understands, written as {@code FUN:name!param} */
    String[] getFunctionInfo();

    /** Runs a function on this device. Server side only, returns are unused for now. */
    String runRORFunction(String name, String[] params);

    /** Extracts the command name from a full signal, e.g. {@code write} out of {@code write!hello}. */
    static String getCommand(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);

        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts[0].isEmpty()) throw new RORFunctionException(EX_NULL);

        return parts[0].toLowerCase(Locale.ROOT);
    }

    /** Extracts the parameter list from a full signal. */
    static String[] getParams(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);

        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts.length == 1) return new String[0];

        return parts[1].split(PARAM_SEPARATOR);
    }

    static int parseInt(String value) {
        return parseInt(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static int parseInt(String value, int min, int max) {
        int result;
        try {
            result = Integer.parseInt(value);
        } catch (NumberFormatException first) {
            try {
                result = (int) Math.round(Double.parseDouble(value));
            } catch (NumberFormatException second) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }

        if (result < min || result > max) throw new RORFunctionException(EX_FORMAT);
        return result;
    }
}
