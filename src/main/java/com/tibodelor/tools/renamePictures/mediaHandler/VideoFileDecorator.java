package com.tibodelor.tools.renamePictures.mediaHandler;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoFileDecorator implements MediaFileDecorator {
    private static final Logger LOG = Logger.getLogger(ImageFileDecorator.class.getName());
    private final Path inputFile;

    private Optional<OffsetDateTime> mediaDate = null;

    public VideoFileDecorator(Path inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public Optional<OffsetDateTime> getMediaDate() {
        if (mediaDate == null) {
            try (IsoFile isoFile = new IsoFile(inputFile.toString())) {
                MovieBox movieBox = isoFile.getMovieBox();
                if (movieBox == null) {
                    LOG.log(Level.FINE, "No movieBox for inputFile {0}", inputFile.toString());
                    mediaDate = Optional.empty();
                    return mediaDate;
                }
                MovieHeaderBox movieHeaderBox = movieBox.getMovieHeaderBox();
                if (movieHeaderBox == null) {
                    LOG.log(Level.FINE, "No movieHeaderBox for inputFile {0}", inputFile.toString());
                    mediaDate = Optional.empty();
                    return mediaDate;
                }
                Date creationTime = movieHeaderBox.getCreationTime();

                if (creationTime == null) {
                    LOG.log(Level.FINE, "No creationTime for inputFile {0}", inputFile.toString());
                    mediaDate = Optional.empty();
                    return mediaDate;
                }
                mediaDate = Optional.of(creationTime.toInstant().atOffset(ZoneOffset.UTC));
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Couldn't read file " + inputFile.toString(), e);
                mediaDate = Optional.empty();
                return mediaDate;
            }
        }
        return mediaDate;
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public Optional<Boolean> isMediaContentIdenticalTo(Path otherFile) {
        return null;
    }
}
