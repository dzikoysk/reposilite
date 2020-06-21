package org.panda_lang.reposilite;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

public abstract class ReposiliteIntegrationTest {

    protected static final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

    @TempDir
    protected File workingDirectory;
    protected Reposilite reposilite;

    @BeforeEach
    protected void before() throws Exception {
        reposilite = ReposiliteLauncher.create(workingDirectory.getAbsolutePath(), true);
        reposilite.launch();
    }

    @AfterEach
    protected void after() throws Exception {
        reposilite.shutdown();
    }

    protected <E extends Exception> void executeOnLocked(File file, ThrowingRunnable<E> runnable) throws E, IOException {
        FileOutputStream out = new FileOutputStream(file);
        FileLock lock = out.getChannel().lock();
        out.write(-1);
        runnable.run();
        lock.release();
        out.close();
    }

    protected HttpResponse get(String uri) throws IOException {
        return requestFactory.buildGetRequest(url(uri))
            .setThrowExceptionOnExecuteError(false)
            .execute();
    }

    protected HttpResponse getAuthenticated(String uri, String username, String password) throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(url(uri));
        request.setThrowExceptionOnExecuteError(false);
        request.getHeaders().setBasicAuthentication(username, password);
        return request.execute();
    }

    protected GenericUrl url(String uri) {
        return new GenericUrl("http://localhost:80" + uri);
    }

}
