package com.tibodelor.tools.renamePictures;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
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

    private static final Set<String> SUPPORTED_IMAGE_EXT = Set.of("jpg", "jpeg", "tif", "cdr");
    private static final Set<String> SUPPORTED_VIDEO_EXT = Set.of("mp4", "3gp", "mov");

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("^.+\\.(\\w{1,4})$");
    private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("YYYY");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");

    private final Path inputDir;
    private final Path outputDir;
    private final Path noDateDir;
    private final Path duplicateDir;

    public RenamePicture(Path inputDir, Path outputDir) {
        if (!Files.isDirectory(inputDir))
            throw new RuntimeException("Can't Find " + inputDir);
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.duplicateDir = outputDir.resolve("_DUP");
        this.noDateDir = outputDir.resolve("Unknown");

    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputDir);
            Files.createDirectories(duplicateDir);
            Files.createDirectories(noDateDir);
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
                        Optional<Date> mediaDate = getMediaDate(inputFile, extension);
                        moveFile(inputFile, extension, mediaDate);
                    }

                    return FileVisitResult.CONTINUE;
                }


                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    super.postVisitDirectory(dir, exc);
                    if (Files.list(dir).count() == 0) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "failed to visit files", e);
        }

    }

    private Optional<Date> getMediaDate(Path inputFile, String extension) {
        if (matchAllowedExtension(SUPPORTED_IMAGE_EXT, extension)) {
            return getImageDate(inputFile);
        } else if (matchAllowedExtension(SUPPORTED_VIDEO_EXT, extension)) {
            return getVideoDate(inputFile);
        } else
            return Optional.empty();
    }

    private Optional<Date> getImageDate(Path inputFile) {
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(inputFile.toFile());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't read file " + inputFile.toString(), e);
            return Optional.empty();
        }
        ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifDirectory == null) {
            LOG.log(Level.WARNING, "No exif directory for inputFile {0}", inputFile.toString());
            return Optional.empty();
        }
        Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        if (date == null) {
            LOG.log(Level.WARNING, "No date tag for inputFile {0}", inputFile.toString());
            return Optional.empty();
        }
        return Optional.of(date);
    }

    private Optional<Date> getVideoDate(Path inputFile) {
        LOG.log(Level.FINER, "Inspecting {0}", inputFile);
        try (IsoFile isoFile = new IsoFile(inputFile.toString())) {
            MovieBox movieBox = isoFile.getMovieBox();
            if (movieBox == null) {
                LOG.log(Level.FINE, "No movieBox for inputFile {0}", inputFile.toString());
                return Optional.empty();
            }
            MovieHeaderBox movieHeaderBox = movieBox.getMovieHeaderBox();
            if (movieHeaderBox == null) {
                LOG.log(Level.FINE, "No movieHeaderBox for inputFile {0}", inputFile.toString());
                return Optional.empty();
            }
            Date creationTime = movieHeaderBox.getCreationTime();

            if (creationTime == null) {
                LOG.log(Level.FINE, "No creationTime for inputFile {0}", inputFile.toString());
                return Optional.empty();
            }
            return Optional.of(creationTime);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Couldn't read file " + inputFile.toString(), e);
            return Optional.empty();
        }
    }

    private void moveFile(Path inputFile, String extension, Optional<Date> date) throws IOException {
        Path destinationFile;
        if (date.isPresent()) {
            OffsetDateTime dateUtc = date.get().toInstant().atOffset(ZoneOffset.UTC);
            String dirName = DIRECTORY_FORMAT.format(dateUtc);
            String fileName = FILE_NAME_FORMAT.format(dateUtc);
            Path destinationDir = Files.createDirectories(outputDir.resolve(dirName));
            destinationFile = destinationDir.resolve(fileName + "." + extension);
            for (int i = 1; destinationFile.toFile().exists(); i++) {
                LOG.log(Level.INFO, "File already exists! {0}", destinationFile.toString());
                destinationDir = Files.createDirectories(duplicateDir.resolve(dirName));
                destinationFile = destinationDir.resolve(fileName + "_" + i + "." + extension);
            }
        } else {
            destinationFile = noDateDir.resolve(inputFile.getFileName());
            for (int i = 1; destinationFile.toFile().exists(); i++) {
                LOG.log(Level.INFO, "File already exists! {0}", destinationFile.toString());
                destinationFile = noDateDir.resolve(i + "_" + inputFile.getFileName() );
            }
        }
        LOG.log(Level.FINE, "Renaming {0} to {1}", new Object[]{inputFile, destinationFile});
        Files.move(inputFile, destinationFile);
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
