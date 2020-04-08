package org.panda_lang.nanomaven.workspace.frontend;

import org.panda_lang.nanomaven.util.FilesUtils;
import org.panda_lang.utilities.commons.FileUtils;

import java.io.File;
import java.io.IOException;

public final class FrontendLoader {

    public Frontend loadFrontend(String frontendFile) throws IOException {
        if (!FilesUtils.fileExists(frontendFile)) {
            FilesUtils.copyResource("/index.html", frontendFile);
        }

        return new Frontend(FileUtils.getContentOfFile(new File(frontendFile)));
    }

}
