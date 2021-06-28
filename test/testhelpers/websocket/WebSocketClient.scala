package testhelpers.websocket

// REF: https://github.com/playframework/play-samples/blob/2.8.x/play-scala-chatroom-example/test/controllers/WebSocketClient.java
// Just converted the java code to scala

import java.util

import play.shaded.ahc.io.netty.handler.codec.http.cookie.Cookie
import play.shaded.ahc.org.asynchttpclient.{AsyncHttpClient, BoundRequestBuilder, ListenableFuture}
import play.shaded.ahc.org.asynchttpclient.netty.ws.NettyWebSocket
import play.shaded.ahc.org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}

import scala.collection.mutable.ListBuffer
import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava

//https://github.com/AsyncHttpClient/async-http-client
class WebSocketClient(val asyncHttpClient: AsyncHttpClient, val url: String) {

  protected val cookies: util.ArrayList[Cookie] = new util.ArrayList()
  val logListener = LogListener


  def call(origin: String = url): Future[NettyWebSocket] = {
    val requestBuilder: BoundRequestBuilder = asyncHttpClient
      .prepareGet(url)
      .addHeader("Origin", origin)
      .setCookies(cookies)
    val handler: WebSocketUpgradeHandler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(logListener).build()
    val javaFuture: ListenableFuture[NettyWebSocket] = requestBuilder.execute(handler)
    FutureConverters.toScala(javaFuture.toCompletableFuture())
  }


  def caughtThrowable() = {
    logListener.getThrowable()
  }


  def addCookie[C <: Cookie](cookie: C): this.type = {
    this.cookies.add(cookie)
    this
  }

  def addCookies[C <: Cookie](cookies: List[C]): this.type = {
    this.cookies.addAll(cookies.asJava)
    this
  }
}
