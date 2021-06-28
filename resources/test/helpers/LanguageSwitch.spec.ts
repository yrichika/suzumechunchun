import LanguageSwitch from '@app/helpers/LanguageSwitch'
import Cookie from '@app/helpers/Cookie'
import Random from '@test/testhelpers/Random'

describe('LanguageSwitch' , () => {
  const en = new Map([
    ['title', 'title'],
    ['desc', 'description']
  ])

  const ja = new Map([
    ['title', 'タイトル'],
    ['desc', '説明']
  ])
  const langSwitch = new LanguageSwitch(en, ja)
  const cookieKey = langSwitch.cookieKey()

  beforeEach(() => {
    document.documentElement.lang = 'en'

    const p1 = document.createElement('p')
    p1.setAttribute('id', 'title')
    p1.setAttribute('class', 'lang-title')
    p1.innerText = 'a'
    const p2 = document.createElement('p')
    p2.setAttribute('id', 'desc')
    p2.setAttribute('class', 'lang-desc')
    p2.innerText = 'b'
    const wrapper = document.createElement('div')
    wrapper.appendChild(p1)
    wrapper.appendChild(p2)
    document.body.appendChild(wrapper)
  })

  test('initLanguage should initialize depends on html lang setting', () => {
    langSwitch.initLanguage()
    expect(document.getElementById('title')?.innerText).toBe(en.get('title'))
    expect(document.getElementById('desc')?.innerText).toBe(en.get('desc'))
  })

  test('setLanguage should set tags text to specified language', () => {
    langSwitch.setLanguage('ja')
    expect(document.getElementById('title')?.innerText).toBe(ja.get('title'))
    expect(document.getElementById('desc')?.innerText).toBe(ja.get('desc'))

    langSwitch.setLanguage('en')
    expect(document.getElementById('title')?.innerText).toBe(en.get('title'))
    expect(document.getElementById('desc')?.innerText).toBe(en.get('desc'))
  })

  test('setLanguage should set cookie language value and document lang to current one', () => {
    langSwitch.setLanguage('ja')
    expect(Cookie.get(cookieKey)).toBe('ja')
    expect(document.documentElement.lang).toBe('ja')

    langSwitch.setLanguage('en')
    expect(Cookie.get(cookieKey)).toBe('en')
    expect(document.documentElement.lang).toBe('en')
  })

  test('it should get default language if cookie has language value', () => {
    const expectedLang = Random.boolean() ? 'ja' : 'en'
    Cookie.set('PLAY_LANG', expectedLang)

    const result = langSwitch.getLanguageSetting()
    expect(result).toBe(expectedLang)

    langSwitch.initLanguage()
    expect(document.documentElement.lang).toBe(expectedLang)

  })

  test('pickLangMessage should pick text based on document lang attribute', () => {
    const enString = Random.string(5)
    const jaString = Random.string(6)

    document.documentElement.lang = 'en'
    const resultEn = LanguageSwitch.pickLangMessage(enString, jaString)
    expect(resultEn).toBe(enString)

    document.documentElement.lang = 'ja'
    const resultJa = LanguageSwitch.pickLangMessage(enString, jaString)
    expect(resultJa).toBe(jaString)
  }) 

  test('pickLangMessage should return empty string if parameters are undefined', () => {
    document.documentElement.lang = Random.boolean() ? 'en' : 'ja'
    const resultEn = LanguageSwitch.pickLangMessage(undefined, undefined)
    expect(resultEn).toBe('')

  }) 
})
