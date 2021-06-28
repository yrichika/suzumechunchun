package testhelpers.websocket

import play.shaded.ahc.org.asynchttpclient.ws.{WebSocket, WebSocketListener}

object LogListener extends WebSocketListener {
  var throwableFound: Throwable = null

  def getThrowable() = {
    throwableFound
  }
  override def onOpen(webSocket: WebSocket): Unit = ()
  override def onClose(webSocket: WebSocket, i: Int, s: String): Unit = ()
  override def onError(throwable: Throwable): Unit = {
    throwableFound = throwable;
  }

}
