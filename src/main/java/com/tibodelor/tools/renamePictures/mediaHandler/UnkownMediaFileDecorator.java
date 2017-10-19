package com.tibodelor.tools.renamePictures.mediaHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnkownMediaFileDecorator implements MediaFileDecorator {

    private static final Logger LOG = Logger.getLogger(ImageFileDecorator.class.getName());

    private final Path inputFile;

    private String checksum = null;

    public UnkownMediaFileDecorator(Path inputFile) {
        this.inputFile = inputFile;
    }


    @Override
    public Optional<OffsetDateTime> getMediaDate() {
        return Optional.empty();
    }

    @Override
    public Path getPath() {
        return inputFile;
    }

    @Override
    public Optional<Boolean> isMediaContentIdenticalTo(Path otherFile) {
        String otherChecksum = null;
        if (checksum == null) {
            try {
                checksum = getFileChecksum(inputFile);
            } catch (IOException | NoSuchAlgorithmException e) {
                LOG.log(Level.WARNING, "Couldn't compute hash for " + inputFile, e);
                return Optional.empty();
            }
        }
        try {
            otherChecksum = getFileChecksum(otherFile);
        } catch (IOException | NoSuchAlgorithmException e) {
            LOG.log(Level.WARNING, "Couldn't compute hash for " + otherFile, e);
            return Optional.empty();
        }

        return Optional.of(checksum.equals(otherChecksum));
    }

    private static String getFileChecksum(Path p) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = Files.newInputStream(p)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();

    }
}
