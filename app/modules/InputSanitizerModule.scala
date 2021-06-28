package modules


import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import play.api.inject._

class InputSanitizerModule extends SimpleModule(
  bind[InputSanitizer].to[JSoupInputSanitizer]
)