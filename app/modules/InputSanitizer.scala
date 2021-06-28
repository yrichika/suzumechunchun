package modules

trait InputSanitizer {
  def sanitize(input: String): String
}
