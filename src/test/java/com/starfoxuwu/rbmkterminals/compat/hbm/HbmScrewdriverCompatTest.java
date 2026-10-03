package com.starfoxuwu.rbmkterminals.compat.hbm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks the seam between this mod and HBM's screwdriver: that the terminals really are {@code IToolable} the way
 * {@code RBMKIndicator} is, and that a screwdriver is the only tool that gets through.
 * <p>
 * HBM is an optional dependency, so it sits on the test runtime classpath only and every type is looked up by name -
 * the assertions would not even load on a machine without the mod, which is exactly the property the mod itself needs.
 * When HBM is missing the test steps aside instead of failing.
 */
public class HbmScrewdriverCompatTest {

    private static final String TOOL_TYPE = "api.hbm.block.IToolable$ToolType";
    private static final String DISPLAY = "com.starfoxuwu.rbmkterminals.compat.hbm.BlockHbmDisplayTerminal";
    private static final String INPUT = "com.starfoxuwu.rbmkterminals.compat.hbm.BlockHbmInputTerminal";
    private static final String DISPLAY_PROBE = "com.starfoxuwu.rbmkterminals.compat.hbm.ScrewdriverProbes$DisplayProbe";
    private static final String INPUT_PROBE = "com.starfoxuwu.rbmkterminals.compat.hbm.ScrewdriverProbes$InputProbe";
    private static final String HELPERS = "com.starfoxuwu.rbmkterminals.compat.hbm.HbmTerminalBlocks";

    private Class<?> toolable;
    private Class<?> toolType;
    private Object screwdriver;
    private Object otherTool;

    @Before
    public void setUp() throws Exception {
        toolable = load("api.hbm.block.IToolable");
        toolType = load(TOOL_TYPE);

        for (Object constant : toolType.getEnumConstants()) {
            if ("SCREWDRIVER".equals(((Enum<?>) constant).name())) {
                screwdriver = constant;
            } else if (otherTool == null) {
                otherTool = constant;
            }
        }

        assertNotNull("HBM's tool list has no screwdriver any more", screwdriver);
        assertNotNull("HBM's tool list has nothing but a screwdriver left", otherTool);
    }

    @Test
    public void bothTerminalsAreHbmToolable() throws Exception {
        assertTrue(toolable.isAssignableFrom(load(DISPLAY)));
        assertTrue(toolable.isAssignableFrom(load(INPUT)));
    }

    @Test
    public void theDisplayTerminalAnswersOnlyTheScrewdriver() throws Exception {
        assertOnlyTheScrewdriverOpensTheWindow(DISPLAY_PROBE);
    }

    @Test
    public void theInputTerminalAnswersOnlyTheScrewdriver() throws Exception {
        assertOnlyTheScrewdriverOpensTheWindow(INPUT_PROBE);
    }

    @Test
    public void onlyTheScrewdriverDrivesTheConfiguration() throws Exception {
        Method drives = load(HELPERS).getDeclaredMethod("drivesConfiguration", toolType);
        drives.setAccessible(true);

        assertEquals(Boolean.TRUE, drives.invoke(null, screwdriver));
        assertEquals(Boolean.FALSE, drives.invoke(null, otherTool));
    }

    /**
     * Runs {@code onScrew} the way HBM's screwdriver would, once with the wrong tool and once with the right one. The
     * probe swallows the window, so no world or player is needed; a {@code null} world also proves the wrong tool is
     * rejected before anything touches it.
     */
    private void assertOnlyTheScrewdriverOpensTheWindow(String probeName) throws Exception {
        Object probe = load(probeName).getDeclaredConstructor()
            .newInstance();
        Method onScrew = probe.getClass()
            .getMethod(
                "onScrew",
                World.class,
                EntityPlayer.class,
                int.class,
                int.class,
                int.class,
                int.class,
                float.class,
                float.class,
                float.class,
                toolType);

        assertFalse(
            "a tool that is not the screwdriver must not be handled",
            (Boolean) onScrew.invoke(probe, null, null, 0, 0, 0, 1, 0F, 0F, 0F, otherTool));
        assertFalse("nothing may open for the wrong tool", opened(probe));

        assertTrue(
            "the screwdriver has to be handled",
            (Boolean) onScrew.invoke(probe, null, null, 0, 0, 0, 1, 0F, 0F, 0F, screwdriver));
        assertTrue("the screwdriver has to open the configuration window", opened(probe));
    }

    private static boolean opened(Object probe) throws Exception {
        Field field = probe.getClass()
            .getField("opened");
        return field.getBoolean(probe);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            Assume.assumeNoException("HBM is not installed", ex);
            throw new AssertionError(ex); // assumeNoException always throws, this only keeps the compiler happy
        }
    }
}
