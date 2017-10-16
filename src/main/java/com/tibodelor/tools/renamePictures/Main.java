package com.tibodelor.tools.renamePictures;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

public class Main {

    static {
        InputStream configFile = Main.class.getResourceAsStream("/logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(configFile);
        } catch (IOException e) {
            System.err.println("Can't Initialize logger");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new RenamePicture(Paths.get("i:/phototagged/photos"), Paths.get("i:/destination")).run();
    }
}
