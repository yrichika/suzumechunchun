import Cookie from '@app/helpers/Cookie'

import Random from '@test/testhelpers/Random'

/**
 * Cookie class functionality depends on browsers.
 * Note that this test is not really testing how it behaves on browsers.
 * This test is treating `document.cookie` as just a string variable.
 * `Cookie.set` appends string with `=` operator on actual browsers.
 * But this test `document.cookie` overrides string like usual variables.
 */
describe('Cookie' , () => {
  const cookieKey: string = 'test'
  const cookieValue: string = Random.string(10)
  Object.defineProperty(window.document, 'cookie', {
    writable: true,
    value: ''
  })

  beforeEach(() => {
    document.cookie = cookieKey + '=' + cookieValue
  });

  test('get should get an item from selected key', () => {
    expect(Cookie.get(cookieKey)).toBe(cookieValue)
  })

  /**
   * This test is not really working.
   * This test overwrites the existing cookie.
   * But it does not overwrite when running on browser.
   */
  test('set should be able to set a cookie', () => {

    const key = Random.string(5)
    const value = Random.string(10)
    Cookie.set(key, value)

    expect(Cookie.get(key)).toBe(value)
    // It should still get another item. This code should work fine on browser, but not in test.
    // expect(Cookie.get(cookieKey)).toBe(cookieValue)
  })


  test('set should set default cookie path to root', () => {
    const key = Random.string(5)
    const value = Random.string(10)
    Cookie.set(key, value)

    expect(document.cookie).toMatch(new RegExp('path=/'))
  })


  test('set should set cookie path if specified', () => {
    const key = Random.string(5)
    const value = Random.string(10)
    const path = '/fake-path'
    Cookie.set(key, value, path)

    expect(document.cookie).toMatch(new RegExp(`path=${path}`))
  })

})
