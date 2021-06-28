package testhelpers.websocket
import play.shaded.ahc.io.netty.handler.codec.http.cookie.Cookie

// REF: package play.libs.ws.WSCookieBuilder
final case class TestCookie(var name: String, var value: String) extends Cookie {

  var domain: String = "localhost"
  var wrap: Boolean = false
  var path: String = ""
  var maxAge: Long = 600
  var isSecure: Boolean = false
  var isHttpOnly: Boolean = false

  def setValue(var1: String) = {
    value = var1
  }

  def setWrap(var1: Boolean) ={
    wrap = var1
  }

  def setHttpOnly(var1: Boolean) = {
    isHttpOnly = var1
  }

  def setSecure(var1: Boolean) = {
    isSecure = var1
  }

  def setMaxAge(var1: Long) = {
    maxAge = var1
  }

  def setDomain(var1: String) = {
    domain = var1
  }

  def setPath(var1: String) = {
    path = var1
  }

  /**
   * REF: Comparable interface
   */
  override def compareTo(o: Cookie): Int = {
    if (this.name < o.name) {
      return -1
    }
    if (this.name == o.name) {
      return 0
    }
    1
  }

}
