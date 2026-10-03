package com.starfoxuwu.rbmkterminals;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";

    /** Whether the terminal's selfdestruct command may actually blow up. Off by default, it is a joke command. */
    public static boolean enableTerminalSelfDestruct = false;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        enableTerminalSelfDestruct = configuration.getBoolean(
            "enableTerminalSelfDestruct",
            "terminal",
            enableTerminalSelfDestruct,
            "Allow the input terminal's selfdestruct command to actually explode");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
