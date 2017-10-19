package com.tibodelor.tools.renamePictures;

import com.tibodelor.tools.renamePictures.mediaHandler.ImageFileDecorator;
import com.tibodelor.tools.renamePictures.mediaHandler.MediaFileDecorator;
import com.tibodelor.tools.renamePictures.mediaHandler.UnkownMediaFileDecorator;
import com.tibodelor.tools.renamePictures.mediaHandler.VideoFileDecorator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
                        MediaFileDecorator mediaFile = getMediaDecorator(inputFile, extension);
                        moveFile(mediaFile, extension);
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

    private MediaFileDecorator getMediaDecorator(Path inputFile, String extension) {
        if (matchAllowedExtension(SUPPORTED_IMAGE_EXT, extension)) {
            return new ImageFileDecorator(inputFile);
        } else if (matchAllowedExtension(SUPPORTED_VIDEO_EXT, extension)) {
            return new VideoFileDecorator(inputFile);
        } else
            return new UnkownMediaFileDecorator(inputFile);
    }

    private void moveFile(MediaFileDecorator mediaFile, String extension) throws IOException {
        Optional<OffsetDateTime> mediaDate = mediaFile.getMediaDate();
        if (mediaDate.isPresent()) {
            OffsetDateTime dateUtc = mediaDate.get().toInstant().atOffset(ZoneOffset.UTC);
            String dirName = DIRECTORY_FORMAT.format(dateUtc);
            String fileName = FILE_NAME_FORMAT.format(dateUtc);
            Path destinationDir = Files.createDirectories(outputDir.resolve(dirName));
            tryMoveFile(mediaFile, conflictNumber -> {
                Path actualDestinationDir = destinationDir;
                String conflictString = "";
                if (conflictNumber > 0) {
                    actualDestinationDir = Files.createDirectories(duplicateDir.resolve(dirName));
                    conflictString = "_" + conflictNumber;
                }
                return actualDestinationDir.resolve(fileName + conflictString + "." + extension);
            });
        } else {
            tryMoveFile(mediaFile, conflictNumber -> {
                String conflictString = "";
                if (conflictNumber > 0) {
                    conflictString = conflictNumber + "_";
                }
                return noDateDir.resolve(conflictString + mediaFile.getPath().getFileName());
            });
        }
    }

    private static void tryMoveFile(MediaFileDecorator mediaFile, FileRenamer fileRenamer) throws IOException {
        for (int i = 0; ; i++) {
            Path pathTry = fileRenamer.getPath(i);
            if (Files.exists(pathTry)) {
                if (mediaFile.isMediaContentIdenticalTo(pathTry).orElse(false)) {
                    Files.delete(mediaFile.getPath());
                    LOG.log(Level.INFO, "Files are identical, removing first one [{0}]==[{1}]", new Object[]{mediaFile.getPath(), pathTry});
                    return;
                }
                LOG.log(Level.FINE, "File name already taken! {0}", pathTry.toString());
            } else {
                LOG.log(Level.FINE, "Renaming {0} to {1}", new Object[]{mediaFile.getPath(), pathTry});
                Files.move(mediaFile.getPath(), pathTry);
                return;
            }
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