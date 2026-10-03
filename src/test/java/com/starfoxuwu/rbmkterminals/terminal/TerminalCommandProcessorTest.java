package com.starfoxuwu.rbmkterminals.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.starfoxuwu.rbmkterminals.ror.IRORInteractive;
import com.starfoxuwu.rbmkterminals.ror.RORFunctionException;

/**
 * The command language is pure logic, so it is tested without a running game. {@link ITerminalHost} is what makes that
 * possible, the processor never touches the world itself.
 */
public class TerminalCommandProcessorTest {

    private FakeHost host;
    private TerminalCommandProcessor processor;

    @Before
    public void setUp() {
        host = new FakeHost();
        processor = new TerminalCommandProcessor(host);
    }

    @Test
    public void blankInputIsIgnored() {
        processor.eval("   ");
        processor.eval(null);

        assertEquals(0, host.changes);
        assertTrue(host.isEmpty());
    }

    @Test
    public void commandIsEchoedAndAnswered() {
        processor.eval("echo hello world");

        assertEquals("hello world", host.lines[0]);
        assertEquals("echo hello world", host.lines[1]);
        assertTrue(host.changes > 0);
    }

    @Test
    public void unknownCommandIsReported() {
        processor.eval("nonsense");

        assertTrue(host.lines[0].contains("Unrecognized"));
    }

    @Test
    public void helpIsPrintedTopDown() {
        processor.eval("help");

        assertEquals("Available commands:", host.lines[0]);
        assertEquals("help", host.lastLine());
        assertTrue(host.containsLine("echo"));
        assertTrue(host.containsLine("send"));
        assertTrue(host.containsLine("selfdestruct"));
    }

    @Test
    public void setOverwritesASingleRow() {
        processor.eval("set 4 hello world");

        assertEquals("hello world", host.lines[3]);
        assertTrue(host.lines[0].contains("Row 4 set"));
    }

    @Test
    public void setRejectsRowsOutsideTheScreen() {
        processor.eval("set 99 nope");
        assertTrue(host.lines[0].contains("between 1 and " + ITerminalHost.LINE_COUNT));

        processor.eval("set notANumber nope");
        assertTrue(host.lines[0].contains("Usage: set"));
    }

    @Test
    public void redstoneOutputFollowsTheCommand() {
        processor.eval("rs 7");
        assertEquals(7, host.signal);

        processor.eval("rs 42");
        assertEquals(7, host.signal);
        assertTrue(host.lines[0].contains("between 0 and 15"));

        processor.eval("rs 0");
        assertEquals(0, host.signal);
    }

    @Test
    public void clearWipesTheScreenButKeepsTheAnswer() {
        processor.eval("echo something");
        processor.eval("clear");

        assertEquals("Screen cleared.", host.lines[0]);
        for (int row = 1; row < ITerminalHost.LINE_COUNT; row++) {
            assertEquals("", host.lines[row]);
        }
    }

    @Test
    public void readOutsComeFromTheHost() {
        processor.eval("pos");
        assertEquals("Position: the answer", host.lines[0]);

        processor.eval("time");
        assertEquals("World time: the answer", host.lines[0]);
    }

    @Test
    public void selfDestructHonoursTheConfig() {
        processor.eval("selfdestruct");
        assertEquals(0, host.explosions);
        assertTrue(host.lines[0].contains("disabled"));

        host.selfDestructEnabled = true;
        processor.eval("selfdestruct");
        assertEquals(1, host.explosions);
    }

    @Test
    public void channelCanBeSetAndCleared() {
        processor.eval("chan control");
        assertEquals("control", host.channel);
        assertTrue(host.lines[0].contains("Set channel to control"));

        processor.eval("chan");
        assertEquals("", host.channel);
        assertTrue(host.lines[0].contains("<none>"));
    }

    @Test
    public void channelNamesAreCapped() {
        processor.eval("chan " + "abcdefghijklmnopqrstuvwxyz0123456789");

        assertEquals(TerminalCommandProcessor.MAX_CHANNEL_LENGTH, host.channel.length());
    }

    @Test
    public void sendingWithoutAChannelIsRefused() {
        processor.eval("send write!hello");

        assertEquals(0, host.sent.size());
        assertTrue(host.lines[0].contains("no channel set"));
    }

    @Test
    public void sendPublishesASingleSignal() {
        processor.eval("chan control");
        processor.eval("send write!hello");

        assertEquals(1, host.sent.size());
        assertEquals("write!hello", host.sent.get(0));
        assertTrue(host.lines[0].contains("Sent signal on control"));
    }

    @Test
    public void startAndStopToggleTheRepeater() {
        processor.eval("chan control");
        processor.eval("start rs!7");

        assertEquals("rs!7", host.repeatCommand);
        assertTrue(host.lines[0].contains("Repeating signal on control"));

        processor.eval("stop");
        assertEquals("", host.repeatCommand);
        assertTrue(host.lines[0].contains("Stopping repeat signal"));
    }

    @Test
    public void remoteWriteAndSetAndClearReachTheScreen() {
        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "write", new String[] { "hello", "radio" });
        assertEquals("hello radio", host.lines[0]);

        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "set4", new String[] { "row", "four" });
        assertEquals("row four", host.lines[3]);

        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "clear", new String[0]);
        assertTrue(host.isEmpty());
    }

    @Test
    public void remoteSubmitNeedsAKeyboard() {
        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "submit", new String[] { "echo", "hi" });
        assertTrue(host.lines[0].contains("no keyboard"));
        assertEquals(0, host.submitted);

        host.interactive = true;
        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "submit", new String[] { "echo", "hi" });
        assertEquals(1, host.submitted);
    }

    @Test
    public void unknownRemoteFunctionIsReported() {
        processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "warp", new String[0]);

        assertTrue(host.lines[0].contains("Unknown RoR function"));
    }

    @Test
    public void aRadioSignalRunsTheFunctionItNames() {
        processor.receiveSignal("write!hello radio");
        assertEquals("hello radio", host.lines[0]);

        processor.receiveSignal("set3!row three");
        assertEquals("row three", host.lines[2]);
    }

    /** What makes a terminal a display: anything that is no function of it is shown as the message it is. */
    @Test
    public void aRadioSignalThatNamesNoFunctionIsShownAsText() {
        processor.receiveSignal("reactor outlet 1200K");

        assertEquals("reactor outlet 1200K", host.lines[0]);
        assertTrue(host.changes > 0);
    }

    @Test
    public void aMalformedRadioSignalIsReportedInsteadOfShown() {
        processor.receiveSignal("a!b!c");

        assertEquals(IRORInteractive.EX_NAME, host.lines[0]);
    }

    @Test
    public void aBrokenFunctionParameterIsReportedInsteadOfShown() {
        processor.receiveSignal("set99!text");

        assertEquals(IRORInteractive.EX_FORMAT, host.lines[0]);
    }

    @Test
    public void submitOverTheRadioStillNeedsAKeyboard() {
        processor.receiveSignal("submit!echo hi");

        assertTrue(host.lines[0].contains("no keyboard"));
        assertEquals(0, host.submitted);
    }

    @Test
    public void aBlankRadioSignalIsIgnored() {
        processor.receiveSignal("");
        processor.receiveSignal(null);

        assertEquals(0, host.changes);
    }

    @Test
    public void brokenParametersThrow() {
        try {
            processor.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "setX", new String[] { "text" });
            throw new AssertionError("expected a RORFunctionException");
        } catch (RORFunctionException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void signalParsingMatchesTheHbmFormat() {
        assertEquals("write", IRORInteractive.getCommand("write!hello:world"));
        assertEquals(2, IRORInteractive.getParams("write!hello:world").length);
        assertEquals("hello", IRORInteractive.getParams("write!hello:world")[0]);
        assertEquals(0, IRORInteractive.getParams("clear").length);
        assertEquals(7, IRORInteractive.parseInt("7", 0, 15));

        try {
            IRORInteractive.getCommand("");
            throw new AssertionError("expected a RORFunctionException");
        } catch (RORFunctionException expected) {
            assertEquals(IRORInteractive.EX_NULL, expected.getMessage());
        }

        try {
            IRORInteractive.getCommand("a!b!c");
            throw new AssertionError("expected a RORFunctionException");
        } catch (RORFunctionException expected) {
            assertEquals(IRORInteractive.EX_NAME, expected.getMessage());
        }
    }

    /** Behaves like the display terminal: a plain 18 row log with row 0 at the top. */
    private static class FakeHost implements ITerminalHost {

        private final String[] lines = new String[LINE_COUNT];
        private final java.util.List<String> sent = new java.util.ArrayList<String>();

        private int signal;
        private int explosions;
        private int changes;
        private int submitted;
        private boolean selfDestructEnabled;
        private boolean interactive;
        private String channel = "";
        private String repeatCommand = "";

        FakeHost() {
            clearLines();
        }

        @Override
        public void pushLine(String text) {
            for (int i = LINE_COUNT - 1; i > 0; i--) {
                lines[i] = lines[i - 1];
            }
            lines[0] = text == null ? "" : text;
        }

        @Override
        public void clearLines() {
            for (int i = 0; i < LINE_COUNT; i++) {
                lines[i] = "";
            }
        }

        @Override
        public String[] getLines() {
            return lines;
        }

        @Override
        public void setLine(int index, String text) {
            if (index >= 0 && index < LINE_COUNT) lines[index] = text;
        }

        @Override
        public int getSignal() {
            return signal;
        }

        @Override
        public void setSignal(int newSignal) {
            signal = newSignal;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public void setChannel(String newChannel) {
            channel = newChannel == null ? "" : newChannel;
        }

        @Override
        public String getRepeatCommand() {
            return repeatCommand;
        }

        @Override
        public void setRepeatCommand(String command) {
            repeatCommand = command == null ? "" : command;
        }

        @Override
        public void broadcastSignal(String signal) {
            sent.add(signal);
        }

        @Override
        public void submit(String command) {
            submitted++;
        }

        @Override
        public boolean isInteractive() {
            return interactive;
        }

        @Override
        public String query(String key) {
            return "the answer";
        }

        @Override
        public boolean isSelfDestructEnabled() {
            return selfDestructEnabled;
        }

        @Override
        public void selfDestruct() {
            explosions++;
        }

        @Override
        public void markChanged() {
            changes++;
        }

        boolean isEmpty() {
            for (String line : lines) {
                if (!line.isEmpty()) return false;
            }
            return true;
        }

        boolean containsLine(String needle) {
            for (String line : lines) {
                if (line.contains(needle)) return true;
            }
            return false;
        }

        String lastLine() {
            for (int i = LINE_COUNT - 1; i >= 0; i--) {
                if (!lines[i].isEmpty()) return lines[i];
            }
            return "";
        }
    }
}
