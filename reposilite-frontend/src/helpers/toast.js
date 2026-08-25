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

import { createToast } from 'mosha-vue-toastify'

const createInfoToast = (message) =>
  createToast(message, { type: 'info' })

const createSuccessToast = (message) =>
  createToast(message, { type: 'success' })

const createWarningToast = (message) =>
  createToast(message, { type: 'warning' })

const createErrorToast = (message) =>
  createToast(message, { type: 'danger' })

export {
  createInfoToast,
  createSuccessToast,
  createWarningToast,
  createErrorToast
}