package testhelpers.websocket

import com.typesafe.config.ConfigFactory
import play.api.inject.bind
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder

/**
 * REFACTOR: this class is just copy-and-pasted from TestDatabaseConfiguration and TestCase
 */
object WebSocketApp {

  lazy val conf = ConfigFactory.load()

  val slickProfileKey = "slick.dbs.default.profile"
  val slickDriverKey = "slick.dbs.default.db.driver"
  val slickUrlKey = "slick.dbs.default.db.url"

  val testDbProfileKey = "test.db.profile"
  val testDbDriverKey = "test.db.driver"
  val testDbUrl = "test.db.url"


  def setAppConfiguration() = {
    Map(
      slickProfileKey -> conf.getString(testDbProfileKey),
      slickDriverKey -> conf.getString(testDbDriverKey),
      slickUrlKey -> conf.getString(testDbUrl)
    )
  }

  def create() = {
    new GuiceApplicationBuilder().configure(setAppConfiguration()).build()
  }

  /**
   * Injecting cacheApi to any controllers, and overrides
   * AsyncCacheApi instance in the controller.
   * This is helpful when you want handle cache data
   * from test code.
   */
  def createWithCache(cacheApi: AsyncCacheApi) = {
    new GuiceApplicationBuilder()
      .configure(setAppConfiguration())
      .overrides(bind[AsyncCacheApi].toInstance(cacheApi))
      .build()
  }
}
