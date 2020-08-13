/**
 * Replace value with defaultValue if value is undefined
 * @param {*} value value to check
 * @param {*} defaultValue returned if value is undefined
 */
export default function (value, defaultValue) {
  return value === undefined ? defaultValue : value
}
