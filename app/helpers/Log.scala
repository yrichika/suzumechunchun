package helpers

import play.api.{Environment, Logger, Mode}


object Log {
  val file: Logger = Logger("file")
  val console: Logger = Logger("application")

  /**
   * Log only in development mode
   * Need to pass Environment instance. You can just inject env in controllers.
   */
  def devOnly(message: String)(implicit env: Environment) = {
    if (env.mode == Mode.Dev) {
      console.debug(message)
    }
  }
}
