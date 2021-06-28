package testhelpers.websocket

import com.typesafe.config.ConfigFactory
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.ConfigLoader
import play.api.libs.json.Json
import testhelpers.utils.TestRandom

// https://github.com/pauldijou/jwt-scala
// http://pauldijou.fr/jwt-scala/samples/jwt-play-json/

object JwtMaker {

  val header = Json.obj("alg" -> "HS256")


  val key = ConfigFactory.load().getString("play.http.secret.key")


  def hostSession(hostId: String, secretKey: String) = {
    val hostData = Json.obj(
      "data" ->
        Json.obj("hostId" -> hostId,
          "secretKeyHost" -> secretKey),
      "nbf" -> 0,
      "iat"-> Int.MaxValue
    )
    JwtJson.encode(header, hostData, key)
  }

  def clientSession(requestClientId: String, authenticatedClientId: String): String = {
    val clientData = Json.obj(
      "data" ->
        Json.obj("requestClientId" -> requestClientId,
          "authenticatedClientId" -> authenticatedClientId),
      "nbf" -> 0,
      "iat" -> Int.MaxValue
    )
    JwtJson.encode(header, clientData, key)
  }

  def decode(token: String) = {
    JwtJson.decode(token, key, Seq(JwtAlgorithm.HS256))
  }



}
