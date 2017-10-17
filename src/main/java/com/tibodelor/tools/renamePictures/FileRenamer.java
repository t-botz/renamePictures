package com.tibodelor.tools.renamePictures;

import java.io.IOException;
import java.nio.file.Path;

public interface FileRenamer {

    Path getPath(int conflictNumber) throws IOException;
}
