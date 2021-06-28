package helpers.slicktable

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.immutable.Seq
import scala.concurrent.Future


trait CommonQueries {
  this: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  // Need to specify them
  type Model
  type ModelsTable <: Table[Model]
  protected val query: slick.lifted.TableQuery[ModelsTable]
  protected val tableName: String

  /**
   * Same as `SELECT *`
   */
  def all(): Future[Seq[Model]] = db.run(query.result)

  /**
   * Alias for insert method
   *
   * Intended to insert models with factory classes.
   */
  def seed(seeds: Seq[Model]) = {
    insert(seeds)
  }

  def insert(many: Seq[Model]) = {
    db.run(query ++= many)
  }

  def truncate() = {
    db.run(query.delete)
  }


}

