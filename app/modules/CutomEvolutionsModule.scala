package modules

import javax.inject.{Inject, Singleton}
import play.api. Environment
import play.api.db.evolutions._
import play.api.libs.Collections
import org.apache.commons.io.FileUtils



/**
 * REF: https://stackoverflow.com/questions/43093432/is-it-possible-to-name-play-evolution-sql-scripts/43135225#43135225
 * play.api.db.evolutions.EvolutionsModule
 * play.api.db.evolutions.EvolutionsApi
 */
@Singleton
class CustomEvolutionsReader @Inject()(environment: Environment) extends EvolutionsReader {

  /**
   * Read the application evolutions.
   *
   * @param db the database name
   */
  def evolutions(db: String): Seq[Evolution] = {

    val upsMarker = """^(#|--).*!Ups.*$""".r
    val downsMarker = """^(#|--).*!Downs.*$""".r

    val UPS = "UPS"
    val DOWNS = "DOWNS"
    val UNKNOWN = "UNKNOWN"

    val mapUpsAndDowns: PartialFunction[String, String] = {
      case upsMarker(_) => UPS
      case downsMarker(_) => DOWNS
      case _ => UNKNOWN
    }

    val isMarker: PartialFunction[String, Boolean] = {
      case upsMarker(_) => true
      case downsMarker(_) => true
      case _ => false
    }

    val folder = environment.getFile(Evolutions.directoryName(db))

    val sqlFiles = folder.listFiles()
      .filter(file => file.getName.indexOf(".sql") > -1)
      .sortBy(file => {
        val fileName = file.getName
        val nameAfterSqlNumber = fileName.split("\\.")(0).split("_").drop(1).mkString("") + ".sql"
        val sqlNumber = fileName.split("\\.")(0).split("_")(0).toInt
        val newPrefix = "%07d".format(sqlNumber)
        newPrefix + nameAfterSqlNumber
      })
      .toSeq

    sqlFiles.zip(1 to sqlFiles.size)
      .map {
        case (file, revision) => {
          // FIXME: replace FileUtils.readFileToString to other Java or Scala native function
          val script = FileUtils.readFileToString(file, "UTF-8")
          val parsed = Collections.unfoldLeft(("", script.split('\n').toList.map(_.trim))) {
            case (_, Nil) => None
            case (context, lines) => {
              val (some, next) = lines.span(l => !isMarker(l))
              Some((next.headOption.map(c => (mapUpsAndDowns(c), next.tail)).getOrElse("" -> Nil),
                context -> some.mkString("\n")))
            }
          }
            .reverse
            .drop(1)
            .groupBy(i => i._1)
            .view.mapValues {
            _.map(_._2).mkString("\n").trim
          }
          Evolution(
            revision,
            parsed.getOrElse(UPS, ""),
            parsed.getOrElse(DOWNS, "")
          )
        }
      }
  }
}
