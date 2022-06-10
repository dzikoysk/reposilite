/*
 * Copyright (c) 2022 dzikoysk
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

import { ref } from "vue"
import { createURL } from "../../helpers/client"

const connection = ref()
const command = ref("")

export default function useConsole() {
  const consoleAddress = createURL("/api/console/sock")
    .replace("https", "wss")
    .replace("http", "ws")

  const isConnected = () =>
    connection.value?.readyState === WebSocket.OPEN

  const close = () => {
    if (isConnected())
      connection.value.close()
  }

  const history = ref([''])
  const historyIdx = ref(0)

  const addCommandToHistory = (command) => {
    if (history.value[history.value.length - 1] == '')
      history.value.pop()
    history.value.push(command)
    historyIdx.value = history.value.length - 1
  }

  const execute = () => {
    addCommandToHistory(command.value)
    connection.value.send(command.value)
    command.value = ''
  }

  const previousCommand = () =>
    traverseHistory(-1)
  
  const nextCommand = () =>
    traverseHistory(1)

  const traverseHistory = (direction) => {
    const currentCommand = command.value
    const commands = history.value
    const lastCommandIdx = commands.length - 1

    if (lastCommandIdx === historyIdx.value && commands[lastCommandIdx] !== currentCommand)
      addCommandToHistory(currentCommand)
    
    historyIdx.value = Math.max(0, Math.min(commands.length - 1, historyIdx.value + direction))
    command.value = history.value[historyIdx.value]
  }

  const onOpen = ref()
  const onMessage = ref()
  const onError = ref()
  const onClose = ref()

  const connect = (token) => {
    try {
      connection.value = new WebSocket(consoleAddress);

      connection.value.onopen = () => {
        connection.value.send(`Authorization:${token.name}:${token.secret}`)
        onOpen?.value()
      }

      connection.value.onmessage = (event) => {
        if (event.data != "keep-alive")
          onMessage?.value(event.data)
      }

      connection.value.onerror = (error) =>
        onError?.value(error)

      connection.value.onclose = () =>
        onClose?.value()

      const keepAliveInterval = setInterval(() => {
        if (isConnected())
          connection?.value?.send("keep-alive")
        else
          clearInterval(keepAliveInterval)
      }, 1000 * 5)
    } catch (error) {
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
    previousCommand,
    nextCommand,
  }
}
