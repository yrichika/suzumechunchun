package database.tables

import javax.inject.Inject
import database.models.{ChannelToken, CreatedChannel}
import helpers.slicktable.CommonQueries
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.sql.{Blob, Timestamp}
import helpers.crypto.Random
import helpers.crypto.Crypter
import com.mohiva.play.silhouette.api.crypto.Hash

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
class ChannelTokens @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
  with CommonQueries
{
  import profile.api._

  type Model = ChannelToken
  type ModelsTable = ChannelTokensTable
  lazy val query = TableQuery[ModelsTable]
  protected val tableName = "channel_tokens"

  /**
   *
   * @param channelId
   * @return returns host channel token, which is a url token for `/host/{random string comes here}`
   */
  def create(channelId: String, channelName: String): Future[CreatedChannel] = {
    val current = new Timestamp(System.currentTimeMillis())
    val ad = channelId

    val channelTokenId = UUID.randomUUID().toString()
    val channelNameEnc = Crypter.encrypt(channelName, ad)

    val hostChannelToken = Random.alphanumeric()
    val hostChannelTokenHash = Hash.sha2(hostChannelToken)

    val loginChannelToken = Random.alphanumeric()
    val loginChannelTokenHash = Hash.sha2(loginChannelToken)
    val loginChannelTokenEnc = Crypter.encrypt(loginChannelToken, ad)

    val clientChannelToken = Random.alphanumeric()
    val clientChannelTokenHash = Hash.sha2(clientChannelToken)
    val clientChannelTokenEnc = Crypter.encrypt(clientChannelToken, ad)

    val secretKey = Random.alphanumeric(32)
    val secretKeyEnc = Some(Crypter.encrypt(secretKey, ad))
    val statement = query returning query.map(_.id) += ChannelToken(0, channelTokenId, channelId, channelNameEnc, hostChannelTokenHash, loginChannelTokenHash, loginChannelTokenEnc, clientChannelTokenHash, clientChannelTokenEnc, secretKeyEnc, current, current)

    val result = db.run(statement)
    result.map(_ => CreatedChannel(hostChannelToken, loginChannelToken, secretKey))
  }

  def getByChannelId(channelId: String): Future[Option[ChannelToken]] = {
    val statement = query.filter(_.channelId === channelId).result.headOption
    db.run(statement)
  }


  def getByHostChannelToken(hostChannelToken: String): Future[Option[ChannelToken]] = {
    val hashedHostChannelToken = Hash.sha2(hostChannelToken)
    val statement = query.filter(_.hostChannelTokenHash === hashedHostChannelToken).result.headOption
    db.run(statement)
  }

  def getByClientChannelToken(clientChannelToken: String): Future[Option[ChannelToken]] = {
    val hashedClientChannelToken = Hash.sha2(clientChannelToken)
    val statement = query.filter(_.clientChannelTokenHash === hashedClientChannelToken).result.headOption
    db.run(statement)
  }

  def getByLoginChannelToken(loginChannelToken: String): Future[Option[ChannelToken]] = {
    val hashedLoginChannelToken = Hash.sha2(loginChannelToken)
    val statement = query.filter(_.loginChannelTokenHash === hashedLoginChannelToken).result.headOption
    db.run(statement)
  }

  // FIXME: Auto update `updatedAt` column will be implemented
  def trashSecretKeyByHostChannelToken(hostChannelToken: String) = {
    val hashedHostChannelToken = Hash.sha2(hostChannelToken)
    val statement = query.filter(_.hostChannelTokenHash === hashedHostChannelToken)
      .map(tokens => (tokens.secretKeyEnc, tokens.updatedAt))
      .update((None, new Timestamp(System.currentTimeMillis())))
    db.run(statement)
  }

  def deleteByChannelIds(channelIds: Seq[String]) = {
    val statement = query.filter(_.channelId.inSetBind(channelIds)).delete
    db.run(statement)
  }


  final protected class ChannelTokensTable(tag: Tag) extends Table[ChannelToken](tag, tableName) {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def channelTokenId = column[String]("channel_token_id")
    def channelId = column[String]("channel_id")
    def channelNameEnc = column[Array[Byte]]("channel_name_enc")
    def hostChannelTokenHash = column[String]("host_channel_token_hash")
    def loginChannelTokenHash = column[String]("login_channel_token_hash")
    def loginChannelTokenEnc = column[Array[Byte]]("login_channel_token_enc")
    def clientChannelTokenHash = column[String]("client_channel_token_hash")
    def clientChannelTokenEnc = column[Array[Byte]]("client_channel_token_enc")
    def secretKeyEnc = column[Option[Array[Byte]]]("secret_key_enc")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt = column[Timestamp]("updated_at")

    def * = (id, channelTokenId, channelId, channelNameEnc, hostChannelTokenHash, loginChannelTokenHash, loginChannelTokenEnc, clientChannelTokenHash, clientChannelTokenEnc, secretKeyEnc, createdAt, updatedAt) <> (ChannelToken.tupled, ChannelToken.unapply)

  }

}
