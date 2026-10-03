package com.starfoxuwu.rbmkterminals.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.input.Keyboard;

import com.starfoxuwu.rbmkterminals.network.ModNetwork;
import com.starfoxuwu.rbmkterminals.network.PacketTerminalConfig;
import com.starfoxuwu.rbmkterminals.terminal.TerminalCommandProcessor;
import com.starfoxuwu.rbmkterminals.terminal.TerminalConfig;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The window HBM's screwdriver opens on a terminal, the counterpart of {@code GUIScreenRBMKIndicator}.
 * <p>
 * It edits the three things a terminal keeps about itself: the RoR channel it sits on, the colour of its text and
 * whether it reacts to what arrives on that channel. Like in HBM nothing is applied while typing, the SAVE button
 * sends the whole set to the server in one tag, which then validates it and pushes the result back to the clients.
 */
@SideOnly(Side.CLIENT)
public class GuiTerminalConfig extends GuiScreen {

    private static final int WINDOW_WIDTH = 260;
    private static final int WINDOW_HEIGHT = 150;

    private static final int COLOR_TEXT = 0x00E000;
    private static final int COLOR_TITLE = 0x7CFF7C;
    private static final int COLOR_HINT = 0x4C8C4C;
    private static final int COLOR_ERROR = 0xC04040;

    private static final int SWATCH_SIZE = 10;
    private static final int SWATCH_GAP = 3;
    private static final int SWATCH_LEFT = 144;
    private static final int SWATCH_TOP = 56;

    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_TOP = 104;

    /** How long the "saved" marker stays on screen. */
    private static final long SAVED_MILLIS = 1500L;

    private final TileEntityTerminalBase terminal;

    private GuiTextField channel;
    private GuiTextField color;
    private boolean listening;
    private long savedAt;

    public GuiTerminalConfig(TileEntityTerminalBase terminal) {
        this.terminal = terminal;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        listening = terminal.isListening();

        int left = windowLeft(), top = windowTop();
        channel = new GuiTextField(fontRendererObj, left + 70, top + 22, channelWidth(), 14);
        channel.setMaxStringLength(TerminalCommandProcessor.MAX_CHANNEL_LENGTH);
        channel.setText(terminal.getChannel());
        channel.setTextColor(COLOR_TEXT);

        color = new GuiTextField(fontRendererObj, left + 70, top + 54, 60, 14);
        color.setMaxStringLength(TerminalConfig.COLOR_DIGITS);
        color.setText(TerminalConfig.formatColor(currentColor()));
        color.setTextColor(COLOR_TEXT);

        channel.setFocused(true);
    }

    @Override
    public void updateScreen() {
        if (channel != null) channel.updateCursorCounter();
        if (color != null) color.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (terminal.isInvalid()) { // the terminal was broken while the window was open
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
            return;
        }

        int left = windowLeft(), top = windowTop(), winWidth = windowWidth(), winHeight = windowHeight();

        drawRect(0, 0, width, height, 0xC0000000);
        drawRect(left, top, left + winWidth, top + winHeight, 0xF00A120A);
        drawRect(left, top, left + winWidth, top + 14, 0xFF173017);
        drawRect(left + 1, top + 14, left + winWidth - 1, top + 15, 0xFF2E5A2E);
        drawRect(left + 6, top + 100, left + winWidth - 6, top + 101, 0xFF2E5A2E);

        String title = terminal.isInteractive() ? "INPUT TERMINAL CONFIG" : "DISPLAY TERMINAL CONFIG";
        fontRendererObj.drawString(title, left + 6, top + 3, COLOR_TITLE);
        String close = "[ESC] close";
        fontRendererObj
            .drawString(close, left + winWidth - 6 - fontRendererObj.getStringWidth(close), top + 3, COLOR_HINT);

        fontRendererObj.drawString("CHANNEL", left + 8, top + 26, COLOR_TEXT);
        fontRendererObj.drawString("COLOUR", left + 8, top + 58, COLOR_TEXT);
        fontRendererObj.drawString("LISTEN", left + 8, top + 79, COLOR_TEXT);

        fontRendererObj.drawString("empty = off the air", left + 70, top + 40, COLOR_HINT);

        channel.drawTextBox();
        color.drawTextBox();

        int hovered = swatchAt(mouseX, mouseY);
        for (int i = 0; i < TerminalConfig.COLOR_PRESETS.length; i++) {
            int x = left + SWATCH_LEFT + i * (SWATCH_SIZE + SWATCH_GAP), y = top + SWATCH_TOP;
            int preset = TerminalConfig.COLOR_PRESETS[i];
            drawRect(x - 1, y - 1, x + SWATCH_SIZE + 1, y + SWATCH_SIZE + 1, i == hovered ? 0xFFFFFFFF : 0xFF2E5A2E);
            drawRect(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, 0xFF000000 | preset);
        }

        drawToggle(left + 70, top + 73, listening);
        fontRendererObj
            .drawString("react to channel traffic", left + 90, top + 79, listening ? COLOR_TEXT : COLOR_HINT);

        drawButton(mouseX, mouseY, left + (winWidth - BUTTON_WIDTH) / 2, top + BUTTON_TOP, "SAVE");
        if (System.currentTimeMillis() - savedAt < SAVED_MILLIS) {
            fontRendererObj
                .drawString("saved", left + (winWidth + BUTTON_WIDTH) / 2 + 8, top + BUTTON_TOP + 5, COLOR_TITLE);
        }

        String note = colorNote();
        fontRendererObj.drawString(truncate(note, winWidth - 16), left + 8, top + 132, COLOR_HINT);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);

        int left = windowLeft(), top = windowTop();

        int preset = swatchAt(mouseX, mouseY);
        if (preset >= 0) {
            color.setText(TerminalConfig.formatColor(TerminalConfig.COLOR_PRESETS[preset]));
            color.setCursorPositionEnd();
            return;
        }

        if (inside(mouseX, mouseY, left + 70, top + 73, 12, 12)) {
            listening = !listening;
            return;
        }

        int buttonLeft = left + (windowWidth() - BUTTON_WIDTH) / 2;
        if (inside(mouseX, mouseY, buttonLeft, top + BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            save();
            return;
        }

        channel.mouseClicked(mouseX, mouseY, button);
        color.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            // nothing was applied yet, so closing the window simply throws the edits away
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
            return;
        }

        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            save();
            return;
        }

        if (key == Keyboard.KEY_TAB) {
            boolean toColor = channel.isFocused();
            channel.setFocused(!toColor);
            color.setFocused(toColor);
            return;
        }

        if (channel.textboxKeyTyped(character, key)) return;
        if (color.textboxKeyTyped(character, key)) return;
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

    /** Hands everything the window edited to the server in a single tag, exactly like HBM's control packets do. */
    private void save() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(
            TerminalConfig.KEY_CHANNEL,
            channel.getText()
                .trim());

        String text = color.getText()
            .trim();
        int value = TerminalConfig.parseColor(text);
        if (!TerminalConfig.hasColor(value)) {
            // an empty field asks for the terminal's own default, a typo just keeps the colour that is already there
            value = text.isEmpty() ? TerminalConfig.NO_COLOR : terminal.getTextColor();
        }
        if (TerminalConfig.hasColor(value)) data.setInteger(TerminalConfig.KEY_COLOR, value);
        data.setBoolean(TerminalConfig.KEY_LISTENING, listening);

        ModNetwork.getChannel()
            .sendToServer(new PacketTerminalConfig(terminal.xCoord, terminal.yCoord, terminal.zCoord, data));
        savedAt = System.currentTimeMillis();
    }

    /** @return the colour the screen currently paints with, defaults included */
    private int currentColor() {
        return terminal.hasTextColor() ? terminal.getTextColor()
            : TerminalConfig.defaultColor(terminal.isInteractive());
    }

    private String colorNote() {
        String text = color.getText()
            .trim();
        if (text.isEmpty()) return "colour: default, "
            + TerminalConfig.formatColor(TerminalConfig.defaultColor(terminal.isInteractive()));
        if (!TerminalConfig.hasColor(TerminalConfig.parseColor(text))) return "colour: six hex digits, e.g. 00FF00";
        return "REPEAT / redstone output still recolour the text while they are on";
    }

    private void drawToggle(int left, int top, boolean on) {
        drawRect(left, top, left + 12, top + 12, 0xFF2E5A2E);
        drawRect(left + 1, top + 1, left + 11, top + 11, 0xFF0A120A);
        if (on) drawRect(left + 3, top + 3, left + 9, top + 9, COLOR_TEXT);
    }

    private void drawButton(int mouseX, int mouseY, int left, int top, String label) {
        boolean hovered = inside(mouseX, mouseY, left, top, BUTTON_WIDTH, BUTTON_HEIGHT);
        drawRect(left, top, left + BUTTON_WIDTH, top + BUTTON_HEIGHT, hovered ? 0xFF2E5A2E : 0xFF1B331B);
        drawRect(left + 1, top + 1, left + BUTTON_WIDTH - 1, top + BUTTON_HEIGHT - 1, 0xFF0A120A);
        fontRendererObj.drawString(
            label,
            left + (BUTTON_WIDTH - fontRendererObj.getStringWidth(label)) / 2,
            top + 5,
            hovered ? COLOR_TITLE : COLOR_TEXT);
    }

    /** @return the index of the preset under the mouse, or -1 */
    private int swatchAt(int mouseX, int mouseY) {
        int left = windowLeft(), top = windowTop();
        for (int i = 0; i < TerminalConfig.COLOR_PRESETS.length; i++) {
            int x = left + SWATCH_LEFT + i * (SWATCH_SIZE + SWATCH_GAP), y = top + SWATCH_TOP;
            if (inside(mouseX, mouseY, x, y, SWATCH_SIZE, SWATCH_SIZE)) return i;
        }
        return -1;
    }

    private static boolean inside(int mouseX, int mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** The channel field grows with the window, a narrow screen must not push it out of the frame. */
    private int channelWidth() {
        return Math.max(80, windowWidth() - 70 - 14);
    }

    private int windowWidth() {
        return Math.min(WINDOW_WIDTH, Math.max(220, width - 16));
    }

    private int windowHeight() {
        return Math.min(WINDOW_HEIGHT, Math.max(120, height - 16));
    }

    private int windowLeft() {
        return (width - windowWidth()) / 2;
    }

    private int windowTop() {
        return (height - windowHeight()) / 2;
    }

    /** Shortens a string until it fits into the given width in pixels. */
    private String truncate(String text, int maxWidth) {
        if (fontRendererObj.getStringWidth(text) <= maxWidth) return text;

        StringBuilder builder = new StringBuilder();
        int used = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int charWidth = fontRendererObj.getCharWidth(character);
            if (used + charWidth > maxWidth) break;
            used += charWidth;
            builder.append(character);
        }
        return builder.toString();
    }
}
