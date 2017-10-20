package com.tibodelor.tools.renamePictures.mediaHandler;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.tibodelor.tools.renamePictures.PathUtils.getPath;
import static org.junit.jupiter.api.Assertions.*;

class UnkownMediaFileDecoratorTest {
    @Test
    void getMediaDate() throws URISyntaxException {
        UnkownMediaFileDecorator decorator = new UnkownMediaFileDecorator(getPath("/sampleFiles/random.txt"));

        assertFalse(decorator.getMediaDate().isPresent());
    }

    @Test
    void isMediaContentIdenticalTo() throws URISyntaxException {
        UnkownMediaFileDecorator decorator = new UnkownMediaFileDecorator(getPath("/sampleFiles/random.txt"));
        Path otherFile = getPath("/sampleFiles/random_same.txt");

        assertTrue(decorator.isMediaContentIdenticalTo(otherFile).get());
    }

    @Test
    void isMediaContentDifferentTo() throws URISyntaxException {
        UnkownMediaFileDecorator decorator = new UnkownMediaFileDecorator(getPath("/sampleFiles/random.txt"));
        Path otherFile = getPath("/sampleFiles/random_diff.txt");

        assertFalse(decorator.isMediaContentIdenticalTo(otherFile).get());
    }

}