package database.factories

import java.sql.Timestamp
import database.models.ClientLoginRequest

import scala.util.Random

trait ClientLoginRequestFactory extends ModelFactory[ClientLoginRequest]{
  override def define: ClientLoginRequest = {
    ClientLoginRequest(
      0,
      clientLoginRequestId,
      requestClientIdHash,
      requestClientIdEnc,
      channelId,
      codenameEnc,
      passphraseEnc,
      isAuthenticated,
      createdAt,
      updatedAt
    )
  }

  def clientLoginRequestId: String = Random.alphanumeric.take(5).mkString
  def requestClientIdHash: String = Random.alphanumeric.take(5).mkString
  def requestClientIdEnc: Array[Byte] = Array(Random.nextInt(1000).toByte)
  def channelId: String = Random.alphanumeric.take(5).mkString
  def codenameEnc: Option[Array[Byte]] = Some(Array(Random.nextInt(1000).toByte))
  def passphraseEnc: Option[Array[Byte]] = Some(Array(Random.nextInt(1000).toByte))
  def isAuthenticated: Option[Boolean] = None
  def createdAt: Timestamp =  new Timestamp(System.currentTimeMillis())
  def updatedAt: Timestamp =  new Timestamp(System.currentTimeMillis())

}
