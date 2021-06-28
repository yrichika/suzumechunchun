package database.factories

import java.sql.Timestamp
import database.models.AuthenticatedClient

import scala.util.Random

trait AuthenticatedClientFactory extends ModelFactory[AuthenticatedClient]{

  override def define = {
    AuthenticatedClient(0, authenticatedClientIdHash, authenticatedClientIdEnc, requestClientIdHash,
    channelId, createdAt, updatedAt)
  }

  // def id = 0 // auto increment cannot be override by seeding
  def authenticatedClientIdHash = Random.alphanumeric.take(5).mkString
  def authenticatedClientIdEnc = Array(Random.nextInt(1000).toByte)
  def requestClientIdHash = Random.alphanumeric.take(5).mkString
  def channelId = Random.alphanumeric.take(5).mkString
  def createdAt = new Timestamp(System.currentTimeMillis())
  def updatedAt = new Timestamp(System.currentTimeMillis())

}
