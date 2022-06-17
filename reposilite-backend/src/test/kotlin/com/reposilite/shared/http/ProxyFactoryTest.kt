package com.reposilite.shared.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyFactoryTest {

    @Test
    fun `should create http factory`() {
        val proxy = createHttpProxy("http 127.0.0.1:1081")

        assertNotNull(proxy)
        assertEquals(Proxy.Type.HTTP, proxy!!.type())

        val address = proxy.address() as InetSocketAddress
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(1081, address.port)
    }

    @Test
    fun `should create socks factory`() {
        val proxy = createHttpProxy("socks 127.0.0.1:1080 login password")

        assertNotNull(proxy)
        assertEquals(Proxy.Type.SOCKS, proxy!!.type())

        val address = proxy.address() as InetSocketAddress
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(1080, address.port)
    }

}
