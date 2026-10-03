package com.starfoxuwu.rbmkterminals.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import com.starfoxuwu.rbmkterminals.network.ModNetwork;
import com.starfoxuwu.rbmkterminals.network.PacketTerminalCommand;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The keyboard of the input terminal.
 * <p>
 * The screen the world shows is the real thing, this window only offers a readable copy of the log plus the line that
 * is currently being typed, which the renderer mirrors onto the top row of the in world screen.
 */
@SideOnly(Side.CLIENT)
public class GuiTerminal extends GuiScreen {

    private static final int MAX_WIDTH = 340;
    /** Tall enough for all 17 log rows plus the title and the input line on a 240 pixel high GUI. */
    private static final int MAX_HEIGHT = 216;
    private static final String PREFIX = "> ";
    private static final String HINT = "help | echo | write | set | clear | rs | chan | send | start | stop";

    private static final int COLOR_TEXT = 0x00E000;
    private static final int COLOR_TITLE = 0x7CFF7C;
    private static final int COLOR_HINT = 0x4C8C4C;
    private static final int COLOR_CHANNEL = 0x8C8C4C;
    private static final int COLOR_CHANNEL_ACTIVE = 0xFF8000;

    private final List<String> typed = new ArrayList<String>();
    private final TileEntityInputTerminal terminal;

    private GuiTextField input;
    private int historyIndex;

    public GuiTerminal(TileEntityInputTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int inputY = windowTop() + windowHeight() - 22;
        input = new GuiTextField(
            fontRendererObj,
            windowLeft() + 8 + fontRendererObj.getStringWidth(PREFIX),
            inputY,
            Math.max(20, windowWidth() - 24 - fontRendererObj.getStringWidth(PREFIX)),
            8);
        input.setMaxStringLength(TileEntityTerminalBase.MAX_LINE_LENGTH);
        input.setEnableBackgroundDrawing(false);
        input.setFocused(true);
        input.setCanLoseFocus(false);

        historyIndex = typed.size();
    }

    @Override
    public void updateScreen() {
        if (input != null) input.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (terminal.isInvalid()) { // the terminal was broken or blown up while the window was open
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
            return;
        }

        int left = windowLeft(), top = windowTop(), winWidth = windowWidth(), winHeight = windowHeight();
        int inputY = top + winHeight - 22;

        drawRect(0, 0, width, height, 0xC0000000);
        drawRect(left, top, left + winWidth, top + winHeight, 0xF00A120A);
        drawRect(left, top, left + winWidth, top + 14, 0xFF173017);
        drawRect(left + 1, top + 14, left + winWidth - 1, top + 15, 0xFF2E5A2E);
        drawRect(left + 6, inputY - 5, left + winWidth - 6, inputY - 4, 0xFF2E5A2E);

        fontRendererObj.drawString("TERMINAL", left + 6, top + 3, COLOR_TITLE);
        String close = "[ESC] close";
        fontRendererObj
            .drawString(close, left + winWidth - 6 - fontRendererObj.getStringWidth(close), top + 3, COLOR_HINT);

        // the radio state lives in the title bar, the screen itself stays for the log
        String channel = terminal.getChannel();
        String status = "chan: " + (channel.isEmpty() ? "<none>" : channel);
        if (terminal.isRepeating()) status += " [REPEAT]";
        fontRendererObj.drawString(
            truncate(status, Math.max(24, winWidth - 76 - fontRendererObj.getStringWidth(close))),
            left + 66,
            top + 3,
            terminal.isRepeating() ? COLOR_CHANNEL_ACTIVE : COLOR_CHANNEL);

        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            String line = terminal.getLine(row + 1); // row 0 of the screen belongs to the input line
            if (line == null || line.isEmpty()) continue;
            fontRendererObj.drawString(PREFIX + line, left + 8, top + 20 + row * 10, COLOR_TEXT);
        }

        fontRendererObj.drawString(PREFIX, left + 8, inputY, COLOR_TEXT);
        input.drawTextBox();
        fontRendererObj.drawString(HINT, left + 8, top + winHeight - 11, COLOR_HINT);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
            return;
        }

        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            submit();
            return;
        }

        if (key == Keyboard.KEY_UP) {
            recall(-1);
            return;
        }

        if (key == Keyboard.KEY_DOWN) {
            recall(1);
            return;
        }

        if (input.textboxKeyTyped(character, key)) return;
        super.keyTyped(character, key);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void submit() {
        String line = input.getText();
        input.setText("");
        if (line == null || line.trim()
            .isEmpty()) return;

        ModNetwork.getChannel()
            .sendToServer(new PacketTerminalCommand(terminal.xCoord, terminal.yCoord, terminal.zCoord, line.trim()));

        typed.add(line.trim());
        historyIndex = typed.size();
    }

    private void recall(int direction) {
        if (typed.isEmpty()) return;

        historyIndex = Math.max(0, Math.min(typed.size(), historyIndex + direction));
        input.setText(historyIndex >= typed.size() ? "" : typed.get(historyIndex));
        input.setCursorPositionEnd();
    }

    private int windowWidth() {
        return Math.min(MAX_WIDTH, Math.max(200, width - 16));
    }

    private int windowHeight() {
        return Math.min(MAX_HEIGHT, Math.max(96, height - 16));
    }

    private int windowLeft() {
        return (width - windowWidth()) / 2;
    }

    private int windowTop() {
        return (height - windowHeight()) / 2;
    }

    private int visibleRows() {
        return Math.max(1, Math.min(TileEntityTerminalBase.LINE_COUNT - 1, (windowHeight() - 46) / 10));
    }

    /** Shortens a string until it fits into the given width in pixels. */
    private String truncate(String text, int maxWidth) {
        if (fontRendererObj.getStringWidth(text) <= maxWidth) return text;

        StringBuilder builder = new StringBuilder();
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int charWidth = fontRendererObj.getCharWidth(character);
            if (width + charWidth > maxWidth) break;
            width += charWidth;
            builder.append(character);
        }
        return builder.toString();
    }

    /** @return the line that is currently being typed, empty if the keyboard is closed */
    public static String getWorkingLine() {
        GuiTerminal gui = opened();
        return gui == null || gui.input == null ? "" : gui.input.getText();
    }

    /** @return true while the player has the keyboard of some terminal open */
    public static boolean isTyping() {
        GuiTerminal gui = opened();
        return gui != null && gui.input != null && gui.input.isFocused();
    }

    private static GuiTerminal opened() {
        GuiScreen screen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
        return screen instanceof GuiTerminal ? (GuiTerminal) screen : null;
    }
}
