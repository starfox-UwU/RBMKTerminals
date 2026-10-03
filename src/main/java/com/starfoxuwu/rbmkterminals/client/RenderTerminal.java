package com.starfoxuwu.rbmkterminals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.starfoxuwu.rbmkterminals.block.BlockTerminalBase;
import com.starfoxuwu.rbmkterminals.terminal.TerminalConfig;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityInputTerminal;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Paints the terminal screen onto the outer face of the panel, exactly like HBM draws its RBMK terminal.
 * <p>
 * All numbers are in screen pixels: the panel is 250 pixels wide at the scale used here, the text is 8 pixels tall and
 * every row takes 10 pixels, which leaves room for 18 rows.
 */
@SideOnly(Side.CLIENT)
public class RenderTerminal extends TileEntitySpecialRenderer {

    private static final float SCALE = 1F / 250F;
    private static final float ROW_HEIGHT = 10F * SCALE;
    private static final float SCREEN_LEFT = -90F * SCALE;
    private static final float SCREEN_TOP = 89F * SCALE;
    private static final float SCREEN_WIDTH = 180F;

    /** Keeps the text from z-fighting with the texture of the panel it sits on. */
    private static final double SURFACE_OFFSET = 0.002D;

    private static final int COLOR_INPUT_REPEATING = 0xFF8000;
    private static final int COLOR_INPUT_POWERED = 0xFFD000;
    private static final int COLOR_IDLE = 0x1F4A1F;

    private static final String PREFIX = "> ";
    private static final String NO_SIGNAL = "NO SIGNAL";

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityTerminalBase)) return;

        TileEntityTerminalBase terminal = (TileEntityTerminalBase) tile;

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
        GL11.glRotatef(rotation(tile.getBlockMetadata()), 0F, 1F, 0F);
        // local +Z is the direction the screen looks at, the plate hangs on the opposite side of the block
        GL11.glTranslated(0D, 0D, -0.5D + BlockTerminalBase.THICKNESS + SURFACE_OFFSET);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1F, 1F, 1F, 1F);

        drawScreen(terminal);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static int rotation(int metadata) {
        ForgeDirection facing = BlockTerminalBase.getFacing(metadata);
        if (facing == ForgeDirection.EAST) return 90;
        if (facing == ForgeDirection.WEST) return 270;
        if (facing == ForgeDirection.NORTH) return 180;
        return 0;
    }

    private void drawScreen(TileEntityTerminalBase terminal) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        boolean interactive = terminal instanceof TileEntityInputTerminal;
        int logStart = terminal.getLogStart();
        int color = screenColor(terminal);

        String typed = interactive ? GuiTerminal.getWorkingLine() : "";
        boolean typing = interactive && GuiTerminal.isTyping();

        if (isEmpty(terminal) && blink()) {
            drawRow(font, NO_SIGNAL, interactive ? 1 : 0, COLOR_IDLE);
        } else {
            // the input terminal keeps row 0 free for whatever is being typed right now
            int row = interactive ? 1 : 0;
            for (int line = logStart; line < TileEntityTerminalBase.LINE_COUNT; line++) {
                String text = terminal.getLine(line);
                if (!text.isEmpty()) drawRow(font, truncate(font, PREFIX + text), row, color);
                row++;
            }
        }

        if (!interactive) return;

        String text;
        if (typing) {
            text = PREFIX + typed + (blink() ? "_" : "");
        } else if (!typed.isEmpty()) {
            text = PREFIX + typed;
        } else {
            // nothing is being typed, so row 0 falls back to whatever was written there
            String first = terminal.getLine(0);
            if (first.isEmpty()) return;
            text = PREFIX + first;
        }
        drawRow(font, truncate(font, text), 0, color);
    }

    /**
     * The colour the text is painted in. The two states an input terminal can be in keep their own colours so that
     * they stay readable in the world, everything else uses the colour the screwdriver configured, or the default of
     * this terminal type while it has none.
     */
    private static int screenColor(TileEntityTerminalBase terminal) {
        if (terminal instanceof TileEntityInputTerminal) {
            TileEntityInputTerminal input = (TileEntityInputTerminal) terminal;
            if (input.isRepeating()) return COLOR_INPUT_REPEATING;
            if (input.getSignalStrength() > 0) return COLOR_INPUT_POWERED;
        }

        if (terminal.hasTextColor()) return terminal.getTextColor();
        return TerminalConfig.defaultColor(terminal instanceof TileEntityInputTerminal);
    }

    private static boolean isEmpty(TileEntityTerminalBase terminal) {
        for (int row = terminal.getLogStart(); row < TileEntityTerminalBase.LINE_COUNT; row++) {
            if (!terminal.getLine(row)
                .isEmpty()) return false;
        }
        return true;
    }

    private static void drawRow(FontRenderer font, String text, int row, int color) {
        GL11.glPushMatrix();
        GL11.glTranslated(SCREEN_LEFT, SCREEN_TOP - row * ROW_HEIGHT, 0D);
        GL11.glScalef(SCALE, -SCALE, SCALE);
        font.drawString(text, 0, 0, color);
        GL11.glPopMatrix();
    }

    /** Cuts the text off at the right edge of the screen. */
    private static String truncate(FontRenderer font, String text) {
        if (font.getStringWidth(text) <= SCREEN_WIDTH) return text;

        StringBuilder builder = new StringBuilder();
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int charWidth = font.getCharWidth(character);
            if (width + charWidth > SCREEN_WIDTH) break;
            width += charWidth;
            builder.append(character);
        }
        return builder.toString();
    }

    private static boolean blink() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }
}
