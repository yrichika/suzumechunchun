/**
 * example:
 * const sessionStorageMock = new storageMock()
 * Object.defineProperty(window, 'sessionStorage', {
 *   value: sessionStorageMock
 * });
 */
export default class storageMock {

  storage: Map<string, string>

  constructor() {
    this.storage = new Map<string, string>()
  }

  getItem(key: string): null | string {
    const item = this.storage.get(key)
    // Map returns `undefined` if key is not found. But localStorage/sessionStorage
    // returns `null`. Just for type consistency.
    if (item === undefined) {
      return null
    }
    return item
  }

  setItem(key: string, value: string): void {
    this.storage.set(key, value)
  }

  removeItem(key: string): void {
    this.storage.delete(key)
  }

  clear() {
    this.storage.clear()
  }

  keys() {
    return this.storage.keys()
  }

  values() {
    return this.storage.values()
  }
}