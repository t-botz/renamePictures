package com.tibodelor.tools.renamePictures;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenamePicture implements Runnable {

    private static final Logger LOG = Logger.getLogger(RenamePicture.class.getName());

    private static final Set<String> allowedExtensions = Set.of("jpg", "jpeg");

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("^.+\\.(\\w{1,4})$");

    private final Path inputDir;
    private final Path outputDir;

    public RenamePicture(Path inputDir, Path outputDir) {
        if (!Files.isDirectory(inputDir))
            throw new RuntimeException("Can't Find " + inputDir);
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Fail to create outdir", e);
            return;
        }

        try {
            Files.walkFileTree(inputDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matchAllowedExtension(allowedExtensions, file)) {
                        LOG.log(Level.FINER, "Inspecting {0}", file);
                        try {
                            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
                            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                            if (directory == null) {
                                LOG.log(Level.WARNING, "No exif directory for file {0}", file.toString());
                                return FileVisitResult.CONTINUE;
                            }
                            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                            if (date == null) {
                                LOG.log(Level.WARNING, "No exif directory for file {0}", file.toString());
                                return FileVisitResult.CONTINUE;
                            }
                            LOG.log(Level.FINER, "Got date {0} for file {1} ", new Object[]{DateTimeFormatter.ISO_DATE_TIME.format(date.toInstant().atOffset(ZoneOffset.UTC)), file});

                        } catch (ImageProcessingException e) {
                            LOG.log(Level.WARNING, e, () -> "Couldn't process " + file.toString());
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean matchAllowedExtension(Set<String> allowedExtensions, Path pathToMatch) {
        String pathString = pathToMatch.toString();
        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(pathString);
        if (matcher.matches()) {
            String extension = matcher.group(1).toLowerCase();
            boolean isMatching = allowedExtensions.contains(extension);
            LOG.log(Level.FINER, "Is extension {0} matching allowed extensions? {1} , fileName: {2}", new Object[]{extension, isMatching, pathString});
            return isMatching;
        } else {
            LOG.log(Level.FINER, "Couldn't match for an extension, fileName: {0}", pathString);
            return false;
        }

    }
}
