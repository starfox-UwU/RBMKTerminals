package com.starfoxuwu.rbmkterminals.terminal;

import java.util.Locale;

import com.starfoxuwu.rbmkterminals.ror.IRORInteractive;
import com.starfoxuwu.rbmkterminals.ror.RORFunctionException;

/**
 * The command language of the input terminal and the ROR functions both terminals understand, modelled after HBM's
 * {@code TileEntityRBMKTerminal}.
 * <p>
 * Every line the player submits is echoed onto the screen, followed by whatever the command has to say. The processor
 * never talks to the world directly, it only uses the {@link ITerminalHost} it was created with.
 * <p>
 * The other way in is the radio: {@link #receiveSignal} runs what arrives on the channel. A signal that names a
 * function of this terminal is executed, anything else is written onto the screen as the message it is - which is what
 * makes a terminal a display for whatever travels on its channel.
 */
public class TerminalCommandProcessor {

    /** Longest channel name that is accepted, keeps the screen and the packets reasonable. */
    public static final int MAX_CHANNEL_LENGTH = 24;

    private static final String[] HELP = new String[] { "Available commands:", "  help - show this list",
        "  echo <text> - print the text", "  write <text> - append a line to the screen",
        "  set <line> <text> - overwrite a single row (1 - " + ITerminalHost.LINE_COUNT + ")",
        "  clear - wipe the screen", "  rs <0-15> - set the analog redstone output",
        "  chan <name> - tune the RoR channel, without a name to leave it",
        "  send <cmd> - send one RoR command on the channel", "  start <cmd> - repeat a RoR command on the channel",
        "  stop - stop repeating", "  time / pos / dim / players - read outs", "  horse - ?", "  selfdestruct - ?" };

    /** What a terminal without a keyboard can be told to do over the radio. */
    private static final String[] FUNCTIONS = new String[] { IRORInteractive.PREFIX_FUNCTION + "clear",
        IRORInteractive.PREFIX_FUNCTION + "write" + IRORInteractive.NAME_SEPARATOR + "text",
        IRORInteractive.PREFIX_FUNCTION + "set<row>" + IRORInteractive.NAME_SEPARATOR + "text" };

    /** What an input terminal can be told to do, on top of that. */
    private static final String[] INTERACTIVE_FUNCTIONS = new String[] { IRORInteractive.PREFIX_FUNCTION + "clear",
        IRORInteractive.PREFIX_FUNCTION + "write" + IRORInteractive.NAME_SEPARATOR + "text",
        IRORInteractive.PREFIX_FUNCTION + "set<row>" + IRORInteractive.NAME_SEPARATOR + "text",
        IRORInteractive.PREFIX_FUNCTION + "submit" + IRORInteractive.NAME_SEPARATOR + "command" };

    private final ITerminalHost host;

    public TerminalCommandProcessor(ITerminalHost host) {
        this.host = host;
    }

    /** @return the ROR functions a terminal offers, the input terminal having one more than the display terminal */
    public static String[] getFunctionInfo(boolean interactive) {
        return interactive ? INTERACTIVE_FUNCTIONS.clone() : FUNCTIONS.clone();
    }

    /** Parses and executes a single line of input. Blank input is ignored. */
    public void eval(String raw) {
        if (raw == null) return;

        String command = raw.trim();
        if (command.isEmpty()) return;

        host.pushLine(command);

        String head = command;
        String args = "";
        int space = command.indexOf(' ');
        if (space >= 0) {
            head = command.substring(0, space);
            args = command.substring(space + 1)
                .trim();
        }
        head = head.toLowerCase(Locale.ROOT);

        if ("help".equals(head) || "?".equals(head)) {
            pushAll(HELP);
        } else if ("clear".equals(head) || "cls".equals(head)) {
            host.clearLines();
            host.pushLine("Screen cleared.");
        } else if ("echo".equals(head)) {
            if (args.isEmpty()) host.pushLine("Usage: echo <text>");
            else host.pushLine(args);
        } else if ("write".equals(head) || "print".equals(head)) {
            if (args.isEmpty()) host.pushLine("Usage: write <text>");
            else host.pushLine(args);
        } else if ("set".equals(head)) {
            set(args);
        } else if ("rs".equals(head) || "redstone".equals(head)) {
            redstone(args);
        } else if ("chan".equals(head) || "channel".equals(head)) {
            channel(args);
        } else if ("send".equals(head)) {
            send(args);
        } else if ("start".equals(head)) {
            start(args);
        } else if ("stop".equals(head)) {
            stop();
        } else if ("time".equals(head)) {
            host.pushLine("World time: " + host.query("time"));
        } else if ("pos".equals(head)) {
            host.pushLine("Position: " + host.query("pos"));
        } else if ("dim".equals(head) || "dimension".equals(head)) {
            host.pushLine("Dimension: " + host.query("dim"));
        } else if ("players".equals(head)) {
            host.pushLine("Players: " + host.query("players"));
        } else if ("horse".equals(head)) {
            host.pushLine("Horse.");
        } else if ("selfdestruct".equals(head)) {
            if (host.isSelfDestructEnabled()) host.selfDestruct();
            else host.pushLine("Self destruct is disabled in the config.");
        } else {
            host.pushLine("Unrecognized command! Try 'help'.");
        }

        host.markChanged();
    }

    /**
     * Runs a Redstone-over-Radio function on this terminal, the receiving half of the channel feature. Called on the
     * server thread with the name already prefixed, e.g. {@code FUN:write}.
     */
    public String runRORFunction(String name, String[] params) {
        if (name == null) throw new RORFunctionException(IRORInteractive.EX_NULL);

        String allParams = join(params);

        if ((IRORInteractive.PREFIX_FUNCTION + "clear").equals(name)) {
            host.clearLines();
            host.markChanged();
            return null;
        }

        if ((IRORInteractive.PREFIX_FUNCTION + "write").equals(name)) {
            host.pushLine(allParams);
            host.markChanged();
            return null;
        }

        if (name.startsWith(IRORInteractive.PREFIX_FUNCTION + "set")) {
            String row = name.substring(IRORInteractive.PREFIX_FUNCTION.length() + 3);
            int line = IRORInteractive.parseInt(row, 1, ITerminalHost.LINE_COUNT) - 1;
            host.setLine(line, allParams);
            host.markChanged();
            return null;
        }

        if ((IRORInteractive.PREFIX_FUNCTION + "submit").equals(name)) {
            if (!host.isInteractive()) {
                host.pushLine("This terminal has no keyboard.");
                host.markChanged();
                return null;
            }
            host.submit(allParams);
            host.markChanged();
            return null;
        }

        host.pushLine("Unknown RoR function: " + name.substring(IRORInteractive.PREFIX_FUNCTION.length()));
        host.markChanged();
        return null;
    }

    /**
     * The receiving end of the channel: runs one signal that came in over the radio.
     * <p>
     * HBM sends a function as the bare {@code name!param:param}, the {@code FUN:} prefix is added here, exactly like
     * HBM's own controllers do it. Whatever names no function of this terminal is written onto the screen instead, so
     * a terminal tuned to a channel shows plain values and messages too - say what {@code tile.radio_autocal} puts
     * together with its MSES1 {@code send} statement.
     */
    public void receiveSignal(String signal) {
        if (signal == null || signal.isEmpty()) return;

        try {
            String function = IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(signal);

            if (knowsFunction(function)) {
                runRORFunction(function, IRORInteractive.getParams(signal));
                return;
            }
        } catch (RORFunctionException ex) {
            // not even a well formed signal, so there is nothing to run - report it like any other bad input
            host.pushLine(ex.getMessage());
            host.markChanged();
            return;
        }

        host.pushLine(signal);
        host.markChanged();
    }

    /**
     * @return true if that name is one of the functions this terminal offers; a {@code set} whose row does not parse
     *         still counts, so that its error message reaches the screen instead of the raw signal
     */
    private boolean knowsFunction(String name) {
        if ((IRORInteractive.PREFIX_FUNCTION + "clear").equals(name)) return true;
        if ((IRORInteractive.PREFIX_FUNCTION + "write").equals(name)) return true;
        if ((IRORInteractive.PREFIX_FUNCTION + "submit").equals(name)) return true;

        return name.startsWith(IRORInteractive.PREFIX_FUNCTION + "set");
    }

    private void set(String args) {
        int space = args.indexOf(' ');
        String number = space < 0 ? args : args.substring(0, space);
        String text = space < 0 ? ""
            : args.substring(space + 1)
                .trim();

        int line;
        try {
            line = Integer.parseInt(number.trim());
        } catch (NumberFormatException e) {
            host.pushLine("Usage: set <1-" + ITerminalHost.LINE_COUNT + "> <text>");
            return;
        }

        if (line < 1 || line > ITerminalHost.LINE_COUNT) {
            host.pushLine("Row must be between 1 and " + ITerminalHost.LINE_COUNT + ".");
            return;
        }

        // the answer goes out first, writing the row afterwards would push the text one row further down
        host.pushLine("Row " + line + " set.");
        host.setLine(line - 1, text);
    }

    private void redstone(String args) {
        int strength;
        try {
            strength = Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            host.pushLine("Usage: rs <0-15>, currently " + host.getSignal());
            return;
        }

        if (strength < 0 || strength > 15) {
            host.pushLine("Signal strength must be between 0 and 15.");
            return;
        }

        host.setSignal(strength);
        host.pushLine("Redstone output set to " + strength + ".");
    }

    private void channel(String args) {
        String channel = args.length() > MAX_CHANNEL_LENGTH ? args.substring(0, MAX_CHANNEL_LENGTH) : args;
        host.setChannel(channel.trim());
        host.pushLine(
            "Set channel to " + (host.getChannel()
                .isEmpty() ? "<none>" : host.getChannel()));
    }

    private void send(String args) {
        if (args.isEmpty()) {
            host.pushLine("Usage: send <command>");
            return;
        }

        if (host.getChannel()
            .isEmpty()) {
            host.pushLine("Cannot send - no channel set");
            return;
        }

        host.broadcastSignal(args);
        host.pushLine("Sent signal on " + host.getChannel());
    }

    private void start(String args) {
        if (args.isEmpty()) {
            host.pushLine("Usage: start <command>");
            return;
        }

        if (host.getChannel()
            .isEmpty()) {
            host.pushLine("Cannot send - no channel set");
            return;
        }

        host.setRepeatCommand(args);
        host.pushLine("Repeating signal on " + host.getChannel());
    }

    private void stop() {
        host.setRepeatCommand("");
        host.pushLine("Stopping repeat signal");
    }

    private static String join(String[] params) {
        if (params == null || params.length == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (String param : params) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(param);
        }
        return builder.toString();
    }

    /** Pushes the given lines so that they end up on the screen in the very same order. */
    private void pushAll(String... lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            host.pushLine(lines[i]);
        }
    }
}
