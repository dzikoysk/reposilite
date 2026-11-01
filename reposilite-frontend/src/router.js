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

import { createWebHashHistory, createRouter } from "vue-router"
import IndexPage from "./pages/IndexPage.vue"

const readLocation = () => {
  const fragment = window.location.hash.slice(1)
  return fragment && !fragment.startsWith('/') ? `/#${fragment}` : fragment || '/'
}

const toFragment = (path) => path.startsWith('/#') ? path.slice(2) : path

const createViewHistory = () => {
  let currentLocation = readLocation()
  let previousLocation = currentLocation
  const captureLocation = () => {
    previousLocation = currentLocation
    currentLocation = readLocation()
  }
  // Capture view fragments before Vue Router normalizes their leading slash.
  window.addEventListener('popstate', captureLocation, true)
  const history = createWebHashHistory()

  return {
    ...history,
    get location() { return currentLocation },
    get state() { return history.state },
    createHref: path => history.createHref(toFragment(path)),
    push(path, state) {
      history.push(toFragment(path), state)
      currentLocation = path
    },
    replace(path, state) {
      history.replace(toFragment(path), state)
      currentLocation = path
    },
    listen(callback) {
      return history.listen((_, __, info) => {
        if (readLocation() !== currentLocation) history.replace(toFragment(currentLocation))
        callback(currentLocation, previousLocation, info)
      })
    },
    destroy() {
      window.removeEventListener('popstate', captureLocation, true)
      history.destroy()
    }
  }
}

const router = createRouter({
  history: createViewHistory(),
  routes: [
    {
      path: "/:qualifier(.*)",
      name: "Index",
      component: IndexPage,
    }
  ]
})

export default router