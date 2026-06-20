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