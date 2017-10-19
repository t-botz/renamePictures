package com.tibodelor.tools.renamePictures.mediaHandler;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface MediaFileDecorator {

    Optional<OffsetDateTime> getMediaDate();

    Path getPath();

    Optional<Boolean> isMediaContentIdenticalTo(Path otherFile);
}
