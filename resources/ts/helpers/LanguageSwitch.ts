/**
 * Playが言語をcookieのPLAY_LANGに保存するので、localstorageをやめて、cookieから取得して
 * playと共通にした方がいいかもしれない。
 */
import Cookie from './Cookie'

export default class LanguageSwitch {

  languages: Map<string, Map<string, string>> = new Map<string, Map<string, string>>()

  prefix (): string {
    return 'lang-'
  }

    /**
     * To integrate language function with Play,
     * it uses Play's language cookie key.
     * FIXME: maybe I should move this to property and set in constructor.
     */
  cookieKey (): string {
    return 'PLAY_LANG'
  }

  constructor(en: Map<string, string>, ja: Map<string, string>) {
    this.languages.set('en', en)
    this.languages.set('ja', ja)
  }

  getLanguageSetting() {
    return Cookie.get(this.cookieKey()) || document.documentElement.lang
  }

  initLanguage() {
    const docLang = this.getLanguageSetting()
    this.setLanguage(docLang)
  }

  setLanguage(docLang: string) {

    // FIXME: Mapのキーが見つからなかった場合のfallbackが必要。デフォルトはenにするなど。
    const language: Map<string, string> = this.languages.get(docLang) || new Map<string, string>()
    Cookie.set(this.cookieKey(), docLang)
    document.documentElement.lang = docLang
    language.forEach((value, key) => {
      const classes = document.getElementsByClassName(this.prefix() + key)
      if (classes.length <= 0) {
        return
      }
      for (const item of Array.from(classes)) {
        const tag = item as HTMLElement
        tag.innerText = value
      }
    })
  }

  static pickLangMessage(en: string|undefined, ja: string|undefined): string {
    let message = en
    if (document.documentElement.lang == 'ja') {
        message = ja
    }
    if (message == undefined) {
        return ''
    }
    return message
  }

}
