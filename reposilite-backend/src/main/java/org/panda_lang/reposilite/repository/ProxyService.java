package org.panda_lang.reposilite.repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.javalin.http.Context;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.api.ErrorDto;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ProxyService {

    private final Reposilite reposilite;
    private final Configuration configuration;
    private final RepositoryService repositoryService;
    private final FrontendService frontendService;
    private final HttpRequestFactory requestFactory;
    private final ExecutorService proxiedExecutor;

    ProxyService(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.configuration = reposilite.getConfiguration();
        this.repositoryService = reposilite.getRepositoryService();
        this.frontendService = reposilite.getFrontendService();
        this.requestFactory = configuration.proxied.isEmpty() ? null : new NetHttpTransport().createRequestFactory();
        this.proxiedExecutor = configuration.proxied.isEmpty() ? null : Executors.newCachedThreadPool();
    }

    protected Result<CompletableFuture<Context>, ErrorDto> findProxied(Context context) {
        String uri = context.req.getRequestURI();

        // remove repository name if defined
        for (Repository repository : repositoryService.getRepositories()) {
            if (uri.startsWith("/" + repository.getName())) {
                uri = uri.substring(1 + repository.getName().length());
                break;
            }
        }

        // /groupId/artifactId/<content>
        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error(new ErrorDto(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Invalid proxied request"));
        }

        String remoteUri = uri;

        return Result.ok(FutureUtils.submit(reposilite, proxiedExecutor, future -> {
            for (String proxied : configuration.proxied) {
                try {
                    HttpRequest remoteRequest = requestFactory.buildGetRequest(new GenericUrl(proxied + remoteUri));
                    remoteRequest.setThrowExceptionOnExecuteError(false);
                    remoteRequest.setConnectTimeout(3000);
                    remoteRequest.setReadTimeout(10000);
                    HttpResponse remoteResponse = remoteRequest.execute();

                    if (!remoteResponse.isSuccessStatusCode()) {
                        continue;
                    }

                    Long contentLength = remoteResponse.getHeaders().getContentLength();

                    if (contentLength != null && contentLength != 0) {
                        context.res.setContentLengthLong(contentLength);
                    }

                    if (!context.method().equals("HEAD")) {
                        if (configuration.storeProxied) {
                            store(context, remoteUri, remoteResponse);
                        }
                        else {
                            IOUtils.copy(remoteResponse.getContent(), context.res.getOutputStream());
                        }
                    }

                    if (remoteResponse.getContentType() != null) {
                        context.contentType(remoteResponse.getContentType());
                    }

                    return future.complete(context.status(remoteResponse.getStatusCode()));
                } catch (IOException e) {
                    Reposilite.getLogger().warn("Proxied repository " + proxied + " is unavailable: " + e.getMessage());
                } catch (Exception e) {
                    reposilite.throwException(remoteUri, e);
                    future.cancel(true);
                }
            }

            return future.complete(context
                    .status(HttpStatus.SC_NOT_FOUND)
                    .contentType("text/html")
                    .result(frontendService.forMessage("Artifact not found in local and remote repository")));
        }));
    }

    private void store(Context context, String uri, HttpResponse remoteResponse) throws IOException {
        DiskQuota diskQuota = repositoryService.getDiskQuota();

        if (!diskQuota.hasUsableSpace()) {
            Reposilite.getLogger().warn("Out of disk space - Cannot store proxied artifact " + uri);
            return;
        }

        String repositoryName = StringUtils.split(uri, "/")[1]; // skip first path separator
        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            if (!configuration.rewritePathsEnabled) {
                return;
            }

            uri = repositoryService.getPrimaryRepository().getName() + uri;
        }

        File proxiedFile = repositoryService.getFile(uri);
        FileUtils.copyInputStreamToFile(remoteResponse.getContent(), proxiedFile);
        FileUtils.copyFile(proxiedFile, context.res.getOutputStream());
        diskQuota.allocate(proxiedFile.length());

        Reposilite.getLogger().info("Stored proxied " + uri);
    }

    public boolean hasProxiedRepositories() {
        return proxiedExecutor != null;
    }

}
