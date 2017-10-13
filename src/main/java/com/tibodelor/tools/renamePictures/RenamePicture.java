package com.tibodelor.tools.renamePictures;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RenamePicture implements Runnable {

    private static final Logger LOG = Logger.getLogger(RenamePicture.class.getName());

    private final Path inputDir;
    private final Path outputDir;

    public RenamePicture(Path inputDir, Path outputDir) {
        if(!Files.isDirectory(inputDir))
            throw new RuntimeException("Can't Find "+ inputDir);
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            LOG.log(Level.SEVERE,"Fail to create outdir", e);
            return;
        }

        try {
            Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {


                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
