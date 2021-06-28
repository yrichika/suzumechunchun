package database.tables

import javax.inject.Inject
import database.models.Channel
import helpers.slicktable.CommonQueries
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import helpers.crypto.Random
import com.mohiva.play.silhouette.api.crypto.Hash

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class Channels @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
  with CommonQueries
{
  import profile.api._

  type Model = Channel
  type ModelsTable = ChannelsTable
  lazy val query = TableQuery[ModelsTable]
  protected val tableName = "channels"


  def create(hostId: String): Future[String] = {
    val hostIdHashed = Hash.sha2(hostId)
    val channelId = UUID.randomUUID().toString()
    val current = new Timestamp(System.currentTimeMillis())
    val statement = query returning query.map(_.id) += Channel(0, channelId, hostIdHashed, current, current)
    val result = db.run(statement)
    result.map(_ => channelId)
  }

  def getByHostId(hostId: String): Future[Option[Channel]] = {
    val hashedHostId = Hash.sha2(hostId)
    val statement = query.filter(_.hostIdHash === hashedHostId).result.headOption
    db.run(statement)
  }


  def getItemsOlderThan(hour: Int) = {
    val hoursAgo = LocalDateTime.now().minusHours(hour)
    val hoursAgoTimestamp = Timestamp.valueOf(hoursAgo)
    val statement = query.filter(_.createdAt <= hoursAgoTimestamp).result
    db.run(statement)
  }


  def deleteByIds(ids: Seq[String]) = {
    val statement = query.filter(_.channelId.inSetBind(ids)).delete
    db.run(statement)
  }

  final protected class ChannelsTable(tag: Tag) extends Table[Channel](tag, tableName) {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def channelId = column[String]("channel_id")
    def hostIdHash = column[String]("host_id_hash")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt = column[Timestamp]("updated_at")

    def * = (id, channelId, hostIdHash, createdAt, updatedAt) <> (Channel.tupled, Channel.unapply)
  }
}
