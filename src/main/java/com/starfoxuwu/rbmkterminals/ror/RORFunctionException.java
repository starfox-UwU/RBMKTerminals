package com.starfoxuwu.rbmkterminals.ror;

/**
 * Thrown when an ROR command cannot be understood, most of the time because a parameter is not in the expected
 * format. Listeners catch it and put the message on the terminal screen, exactly like HBM does.
 */
public class RORFunctionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RORFunctionException(String message) {
        super(message);
    }
}
