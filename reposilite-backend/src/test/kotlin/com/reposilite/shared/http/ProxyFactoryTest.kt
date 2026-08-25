/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
