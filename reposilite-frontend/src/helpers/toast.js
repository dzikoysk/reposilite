import { createToast } from 'mosha-vue-toastify'

const createInfoToast = (message) =>
  createToast(message, { type: 'info' })

const createSuccessToast = (message) =>
  createToast(message, { type: 'success' })

const createErrorToast = (message) =>
  createToast(message, { type: 'danger' })

export {
  createInfoToast,
  createSuccessToast,
  createErrorToast
}