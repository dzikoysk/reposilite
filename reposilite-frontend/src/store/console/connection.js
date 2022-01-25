/*
 * Copyright (c) 2021 dzikoysk
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

import { ref } from 'vue'
import { createURL } from '../client'

const connection = ref()
const command = ref('')

export default function useConsole() {
  const consoleAddress = createURL('/api/console/sock')
    .replace('https', 'wss')
    .replace('http', 'ws')
  
  const isConnected = () =>
    connection.value?.readyState === WebSocket.OPEN
  
  const close = () => {
    if (isConnected()) {
      connection.value.close()
    }
  }

  const execute = () => {
    connection.value.send(command.value)
    command.value = ''
  }

  const onOpen = ref()
  const onMessage = ref()
  const onError = ref()
  const onClose = ref()
  
  const connect = (token) => {
    try {
      connection.value = new WebSocket(consoleAddress)

      connection.value.onopen = () => {
        connection.value.send(`Authorization:${token.name}:${token.secret}`)
        onOpen?.value()
      }

      connection.value.onmessage = event => {
        if (event.data == 'keep-alive') {
          return
        }

        onMessage?.value(event.data)
      }

      connection.value.onerror = error =>
        onError?.value(error)

      connection.value.onclose = () =>
        onClose?.value()

      const keepAliveInterval = setInterval(() => {
        if (isConnected()) {
          connection?.value?.send('keep-alive')
        }
        else {
          clearInterval(keepAliveInterval)
        }
      }, 1000 * 5)
    } catch (error) {
      console.log(error)
      onError?.value(error)
    }
  }

  return {
    connection,
    connect,
    close,
    onOpen,
    onMessage,
    onError,
    onClose,
    command,
    execute,
  }
}
