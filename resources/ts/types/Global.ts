import LanguageSwitch from '@app/helpers/LanguageSwitch'
import Utils from '@app/helpers/Utils'

export default interface Global {
  document: Document
  window: Window
  L: LanguageSwitch
  Utils: Utils
}
