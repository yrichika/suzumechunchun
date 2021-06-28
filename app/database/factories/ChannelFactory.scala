package database.factories

import database.models.Channel

import scala.util.Random
import java.sql.Timestamp

trait ChannelFactory extends ModelFactory[Channel]{

  override def define = {
    Channel(0, channelId, hostIdHash, createdAt, updatedAt)
  }

  def channelId: String = Random.alphanumeric.take(5).mkString
  def hostIdHash: String = Random.alphanumeric.take(5).mkString
  def createdAt = new Timestamp(System.currentTimeMillis())
  def updatedAt = new Timestamp(System.currentTimeMillis())
}
