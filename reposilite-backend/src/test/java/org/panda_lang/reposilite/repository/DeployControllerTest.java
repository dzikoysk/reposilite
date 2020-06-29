package org.panda_lang.reposilite.repository;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.utilities.commons.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeployControllerTest extends ReposiliteIntegrationTest {

    private final HttpClient client = HttpClients.createDefault();

    @BeforeEach
    void configure() {
        super.reposilite.getTokenService().createToken("/releases/auth/test", "authtest", "secure");
    }

    @Test
    void shouldReturn401AndArtifactDeploymentIsDisabledMessage() throws IOException, AuthenticationException {
        super.reposilite.getConfiguration().setDeployEnabled(false);

        HttpResponse response = put("/releases/groupId/artifactId/file", new UsernamePasswordCredentials("authtest", "secure"), "content");
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());

        String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.contains("Artifact deployment is disabled"));
    }

    @Test
    void shouldReturn401AndInvalidCredentialsMessage() throws IOException, AuthenticationException {
        HttpResponse response = put("/releases/groupId/artifactId/file", new UsernamePasswordCredentials("authtest", "invalid_token"), "content");
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());

        String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.contains("Invalid authorization credentials"));
    }

    private void shouldReturn401AndGivenMessage(String uri, String username, String password, String content, String message) throws IOException, AuthenticationException {
        HttpResponse response = put(uri, new UsernamePasswordCredentials(username, password), content);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());

        String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.contains("Invalid authorization credentials"));
    }

    private HttpResponse put(String uri, UsernamePasswordCredentials credentials, String content) throws IOException, AuthenticationException {
        HttpPut httpPut = new HttpPut(url(uri).toString());
        httpPut.setEntity(new StringEntity(content));
        httpPut.addHeader(new BasicScheme().authenticate(credentials, httpPut, null));
        return client.execute(httpPut);
    }

}