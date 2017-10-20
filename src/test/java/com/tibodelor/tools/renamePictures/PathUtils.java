package com.tibodelor.tools.renamePictures;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static Path getPath(String fileName) throws URISyntaxException {
        return Paths.get(PathUtils.class.getResource(fileName).toURI());
    }
}
