package com.starfoxuwu.rbmkterminals.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.starfoxuwu.rbmkterminals.terminal.TerminalCommandProcessor;
import com.starfoxuwu.rbmkterminals.tileentity.TileEntityTerminalBase;

/**
 * The remote control of both terminals, also the only way to put text onto a display only terminal.
 * <p>
 * It always acts on the terminal the player is looking at, up to eight blocks away.
 */
public class CommandTerminal implements ICommand {

    private static final double RANGE = 8.0D;

    @Override
    public String getCommandName() {
        return "terminal";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("term", "dterm");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/terminal <write|set|clear|chan|read|functions> ...";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object other) {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText("Only a player can aim at a terminal."));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        TileEntityTerminalBase terminal = findTerminal(player);

        if (terminal == null) {
            reply(player, "Look at a terminal block, no further than " + (int) RANGE + " blocks away.");
            return;
        }

        if (args.length == 0) {
            reply(player, getCommandUsage(sender));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String text = join(args, 1);

        if ("write".equals(sub)) {
            if (text.isEmpty()) {
                reply(player, "Usage: /terminal write <text>");
                return;
            }
            terminal.pushLine(text);
            terminal.markChanged();
            reply(player, "Line written.");
        } else if ("set".equals(sub)) {
            writeRow(player, terminal, text);
        } else if ("clear".equals(sub)) {
            terminal.clearLines();
            terminal.markChanged();
            reply(player, "Screen cleared.");
        } else if ("chan".equals(sub) || "channel".equals(sub)) {
            setChannel(player, terminal, text);
        } else if ("read".equals(sub)) {
            read(player, terminal);
        } else if ("functions".equals(sub)) {
            functions(player, terminal);
        } else {
            reply(player, getCommandUsage(sender));
        }
    }

    /** Tunes a terminal, the only way to get a display terminal onto a channel since it has no keyboard. */
    private static void setChannel(EntityPlayer player, TileEntityTerminalBase terminal, String name) {
        if (name.length() > TerminalCommandProcessor.MAX_CHANNEL_LENGTH) {
            name = name.substring(0, TerminalCommandProcessor.MAX_CHANNEL_LENGTH);
        }

        terminal.setChannel(name);
        terminal.markChanged();
        reply(
            player,
            "Channel set to " + (terminal.getChannel()
                .isEmpty() ? "<none>" : terminal.getChannel()));
    }

    private static void functions(EntityPlayer player, TileEntityTerminalBase terminal) {
        reply(player, "RoR functions of this terminal:");
        for (String function : terminal.getFunctionInfo()) {
            reply(player, "  " + function);
        }
    }

    private static void writeRow(EntityPlayer player, TileEntityTerminalBase terminal, String args) {
        int space = args.indexOf(' ');
        String number = space < 0 ? args : args.substring(0, space);
        String text = space < 0 ? ""
            : args.substring(space + 1)
                .trim();

        int row;
        try {
            row = Integer.parseInt(number.trim());
        } catch (NumberFormatException e) {
            reply(player, "Usage: /terminal set <1-" + TileEntityTerminalBase.LINE_COUNT + "> <text>");
            return;
        }

        if (row < 1 || row > TileEntityTerminalBase.LINE_COUNT) {
            reply(player, "Row must be between 1 and " + TileEntityTerminalBase.LINE_COUNT + ".");
            return;
        }

        terminal.setLine(row - 1, text);
        terminal.markChanged();
        reply(player, "Row " + row + " set.");
    }

    private static void read(EntityPlayer player, TileEntityTerminalBase terminal) {
        String channel = terminal.getChannel();
        StringBuilder builder = new StringBuilder("Screen of ").append(terminal.xCoord)
            .append(" / ")
            .append(terminal.yCoord)
            .append(" / ")
            .append(terminal.zCoord)
            .append(" (channel: ")
            .append(channel.isEmpty() ? "none" : channel)
            .append(terminal.isRepeating() ? ", repeating" : "")
            .append("):");

        for (int row = 0; row < TileEntityTerminalBase.LINE_COUNT; row++) {
            String line = terminal.getLine(row);
            if (!line.isEmpty()) builder.append("\n  ")
                .append(row + 1)
                .append(": ")
                .append(line);
        }

        for (String part : builder.toString()
            .split("\n")) {
            reply(player, part);
        }
    }

    private static TileEntityTerminalBase findTerminal(EntityPlayer player) {
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = start.addVector(look.xCoord * RANGE, look.yCoord * RANGE, look.zCoord * RANGE);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(start, end);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;

        TileEntity tile = player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
        return tile instanceof TileEntityTerminalBase ? (TileEntityTerminalBase) tile : null;
    }

    private static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static void reply(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }
}
