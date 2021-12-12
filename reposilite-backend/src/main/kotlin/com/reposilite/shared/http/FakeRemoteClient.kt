package com.reposilite.shared.http

import com.reposilite.journalist.Journalist
import com.reposilite.shared.fs.FileDetails
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.io.InputStream
import java.net.Proxy

private typealias HeadHandler = (String, String?, Int, Int) -> Result<FileDetails, ErrorResponse>
private typealias GetHandler = (String, String?, Int, Int) -> Result<InputStream, ErrorResponse>

class FakeRemoteClientProvider(private val headHandler: HeadHandler, private val getHandler: GetHandler) : RemoteClientProvider {

    override fun createClient(journalist: Journalist, proxy: Proxy?): RemoteClient =
        FakeRemoteClient(headHandler, getHandler)

}

class FakeRemoteClient(private val headHandler: HeadHandler, private val getHandler: GetHandler) : RemoteClient {

    override fun head(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        headHandler(uri, credentials, connectTimeout, readTimeout)

    override fun get(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        getHandler(uri, credentials, connectTimeout, readTimeout)

}