package org.panda_lang.reposilite;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public abstract class ReposiliteIntegrationTest {

    protected final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    protected Reposilite reposilite;

    @BeforeEach
    protected void before() throws Exception {
        this.reposilite = ReposiliteLauncher.create("./src/test/workspace/", true);
        reposilite.launch();
    }

    @AfterEach
    protected void after() {
        reposilite.shutdown();
    }

    protected HttpResponse get(String url) throws IOException {
        return requestFactory.buildGetRequest(url(url)).execute();
    }

    protected GenericUrl url(String url) {
        return new GenericUrl(url);
    }

}
