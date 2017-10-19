package com.tibodelor.tools.renamePictures.mediaHandler;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageFileDecorator implements MediaFileDecorator {
    private static final Logger LOG = Logger.getLogger(ImageFileDecorator.class.getName());
    private final Path inputFile;

    private Optional<OffsetDateTime> mediaDate = null;
    private Metadata metadata;

    public ImageFileDecorator(Path inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public Optional<OffsetDateTime> getMediaDate() {
        loadMetadata();
        return mediaDate;
    }

    private boolean loadMetadata() {
        if (mediaDate == null) {
            try {
                metadata = ImageMetadataReader.readMetadata(inputFile.toFile());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Couldn't read file " + inputFile.toString(), e);
                mediaDate = Optional.empty();
                return true;
            }
            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory == null) {
                LOG.log(Level.WARNING, "No exif directory for inputFile {0}", inputFile.toString());
                mediaDate = Optional.empty();
                return true;
            }
            Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date == null) {
                LOG.log(Level.WARNING, "No date tag for inputFile {0}", inputFile.toString());
                mediaDate = Optional.empty();
                return true;
            }
            mediaDate = Optional.of(date.toInstant().atOffset(ZoneOffset.UTC));
        }
        return false;
    }

    @Override
    public Path getPath() {
        return inputFile;
    }

    @Override
    public Optional<Boolean> isMediaContentIdenticalTo(Path otherFile) {
        loadMetadata();
        return Optional.of(false);
    }
}
