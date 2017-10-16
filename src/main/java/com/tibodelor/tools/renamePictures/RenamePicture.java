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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenamePicture implements Runnable {

    private static final Logger LOG = Logger.getLogger(RenamePicture.class.getName());

    private static final Set<String> allowedExtensions = Set.of("jpg", "jpeg");

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("^.+\\.(\\w{1,4})$");
    private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("YYYY");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");

    private final Path inputDir;
    private final Path outputDir;
    private final Path duplicateDir;

    public RenamePicture(Path inputDir, Path outputDir) {
        if (!Files.isDirectory(inputDir))
            throw new RuntimeException("Can't Find " + inputDir);
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.duplicateDir = outputDir.resolve("_DUP");

    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputDir);
            Files.createDirectories(duplicateDir);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Fail to create outdir", e);
            return;
        }

        try {
            Files.walkFileTree(inputDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path inputFile, BasicFileAttributes attrs) throws IOException {
                    Optional<String> fileExtension = getFileExtension(inputFile);
                    if (fileExtension.isPresent()) {
                        String extension = fileExtension.get();
                        if (matchAllowedExtension(allowedExtensions, extension)) {
                            LOG.log(Level.FINER, "Inspecting {0}", inputFile);
                            try {
                                Metadata metadata = ImageMetadataReader.readMetadata(inputFile.toFile());
                                ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                                if (exifDirectory == null) {
                                    LOG.log(Level.WARNING, "No exif directory for inputFile {0}", inputFile.toString());
                                    return FileVisitResult.CONTINUE;
                                }
                                Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                                if (date == null) {
                                    LOG.log(Level.WARNING, "No exif directory for inputFile {0}", inputFile.toString());
                                    return FileVisitResult.CONTINUE;
                                }

                                OffsetDateTime dateUtc = date.toInstant().atOffset(ZoneOffset.UTC);
                                String dirName = DIRECTORY_FORMAT.format(dateUtc);
                                String fileName = FILE_NAME_FORMAT.format(dateUtc);
                                Path destinationDir = Files.createDirectories(outputDir.resolve(dirName));
                                Path destinationFile = destinationDir.resolve(fileName  + "." + extension);
                                for (int i = 1; destinationFile.toFile().exists(); i++) {
                                    LOG.log(Level.INFO, "File already exists!", destinationFile.toString());
                                    destinationFile = duplicateDir.resolve(fileName+ "_" + i + "." + extension);
                                }

                                LOG.log(Level.FINE, "Renaming {0} to {1}", new Object[]{inputFile, destinationFile});
                                Files.move(inputFile, destinationFile);

                            } catch (ImageProcessingException e) {
                                LOG.log(Level.WARNING, e, () -> "Couldn't process " + inputFile.toString());
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean matchAllowedExtension(Set<String> allowedExtensions, String extensionToMatch) {
        boolean isMatching = allowedExtensions.contains(extensionToMatch);
        LOG.log(Level.FINER, "Is extension {0} matching allowed extensions? {1}", new Object[]{extensionToMatch, isMatching});
        return isMatching;
    }

    private static Optional<String> getFileExtension(Path path) {
        String pathString = path.toString();
        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(pathString);
        if (matcher.matches()) {
            String extension = matcher.group(1).toLowerCase();
            LOG.log(Level.FINER, "Extension {0} found in path {1}", new Object[]{extension, pathString});
            return Optional.of(extension);
        } else {
            LOG.log(Level.FINER, "Couldn't match for an extension, fileName: {0}", pathString);
            return Optional.empty();
        }
    }
}
