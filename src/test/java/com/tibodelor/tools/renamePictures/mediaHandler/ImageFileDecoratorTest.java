package com.tibodelor.tools.renamePictures.mediaHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.tibodelor.tools.renamePictures.PathUtils.getPath;
import static org.junit.jupiter.api.Assertions.*;


class ImageFileDecoratorTest {
    @Test
    void noMediaDate() throws URISyntaxException {
        ImageFileDecorator decorator = new ImageFileDecorator(getPath("/sampleFiles/random.jpg"));
        Assertions.assertFalse(decorator.getMediaDate().isPresent());
    }


    @Test
    void validMediaDate() throws URISyntaxException {
        ImageFileDecorator decorator = new ImageFileDecorator(getPath("/sampleFiles/random_tagged.jpg"));
        OffsetDateTime expectedTime = OffsetDateTime.of(2017, 10, 19, 21, 53, 17, 0, ZoneOffset.UTC);

        assertTrue(decorator.getMediaDate().isPresent());
        assertTrue(decorator.getMediaDate().get().isEqual(expectedTime));
    }

    @Test
    void isMediaContentIdenticalTo() throws URISyntaxException {
        ImageFileDecorator decorator1 = new ImageFileDecorator(getPath("/sampleFiles/random.jpg"));
        Path otherFile = Paths.get(ImageFileDecoratorTest.class.getResource("/sampleFiles/random_tagged.jpg").toURI());

        assertTrue(decorator1.isMediaContentIdenticalTo(otherFile).isPresent());
        assertTrue(decorator1.isMediaContentIdenticalTo(otherFile).get());
    }

}