package com.tibodelor.tools.renamePictures;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        new RenamePicture(Paths.get("/i/phototagged_copy/photos"), Paths.get("/i/destination"));
    }
}
