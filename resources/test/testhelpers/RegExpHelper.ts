export default class RegExpHelper {
  static eitherRegex(eitherThis: string, orThat: string): RegExp {
    return new RegExp('(' + eitherThis + '|' + orThat +')')
  }
}