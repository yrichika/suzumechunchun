package modules


import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

class JSoupInputSanitizer extends InputSanitizer {
  override def sanitize(input: String): String = {
    Jsoup.clean(input, Whitelist.basic())
  }
}

