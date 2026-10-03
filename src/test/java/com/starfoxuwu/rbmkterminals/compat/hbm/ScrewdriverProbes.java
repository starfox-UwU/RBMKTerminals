package com.starfoxuwu.rbmkterminals.compat.hbm;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * Stand ins for the two screwdriver aware terminals, used by {@code HbmScrewdriverCompatTest}.
 * <p>
 * They exist to catch the one step of the screwdriver path that cannot run outside the game: actually opening a window
 * needs a client and a real player standing at the block. Everything before that - is this HBM's screwdriver, is the
 * click handled - is what the test watches instead.
 * <p>
 * Nothing loads these classes unless the test asks for them by name, so a game without HBM never sees them.
 */
public final class ScrewdriverProbes {

    private ScrewdriverProbes() {}

    public static class DisplayProbe extends BlockHbmDisplayTerminal {

        public boolean opened;

        @Override
        protected void openTerminalConfig(World world, EntityPlayer player, int x, int y, int z) {
            opened = true;
        }
    }

    public static class InputProbe extends BlockHbmInputTerminal {

        public boolean opened;

        @Override
        protected void openTerminalConfig(World world, EntityPlayer player, int x, int y, int z) {
            opened = true;
        }
    }
}
