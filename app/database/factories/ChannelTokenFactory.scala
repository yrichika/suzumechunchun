package database.factories

import java.sql.Timestamp
import database.models.ChannelToken

import scala.util.Random

trait ChannelTokenFactory extends ModelFactory[ChannelToken]{
  override def define: ChannelToken = {
    ChannelToken(
      0,
      channelTokenId,
      channelId,
      channelNameEnc,
      hostChannelTokenHash,
      loginChannelTokenHash,
      loginChannelTokenEnc,
      clientChannelTokenHash,
      clientChannelTokenEnc,
      secretKeyEnc,
      createdAt,
      updatedAt
    )
  }

  def channelTokenId: String = Random.alphanumeric.take(5).mkString
  def channelId: String = Random.alphanumeric.take(5).mkString
  def channelNameEnc: Array[Byte] = Array(Random.nextInt(1000).toByte)
  def hostChannelTokenHash: String = Random.alphanumeric.take(5).mkString
  def loginChannelTokenHash: String = Random.alphanumeric.take(5).mkString
  def loginChannelTokenEnc: Array[Byte] = Array(Random.nextInt(1000).toByte)
  def clientChannelTokenHash: String = Random.alphanumeric.take(5).mkString
  def clientChannelTokenEnc: Array[Byte] = Array(Random.nextInt(1000).toByte)
  def secretKeyEnc: Option[Array[Byte]] = Some(Array(Random.nextInt(1000).toByte))
  def createdAt = new Timestamp(System.currentTimeMillis())
  def updatedAt = new Timestamp(System.currentTimeMillis())

}
