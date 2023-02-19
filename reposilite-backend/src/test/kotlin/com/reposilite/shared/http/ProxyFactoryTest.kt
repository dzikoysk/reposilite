package com.reposilite.shared.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyFactoryTest {

    @Test
    fun `should create http factory`() {
        val proxy = createHttpProxy("http 127.0.0.1:1081")

        assertThat(proxy).isNotNull
        assertThat(proxy!!.type()).isEqualTo(Proxy.Type.HTTP)

        val address = proxy.address() as InetSocketAddress
        assertThat(address.hostString).isEqualTo("127.0.0.1")
        assertThat(address.port).isEqualTo(1081)
    }

    @Test
    fun `should create socks factory`() {
        val proxy = createHttpProxy("socks 127.0.0.1:1080 login password")

        assertThat(proxy).isNotNull
        assertThat(proxy!!.type()).isEqualTo(Proxy.Type.SOCKS)

        val address = proxy.address() as InetSocketAddress
        assertThat(address.hostString).isEqualTo("127.0.0.1")
        assertThat(address.port).isEqualTo(1080)
    }

}
