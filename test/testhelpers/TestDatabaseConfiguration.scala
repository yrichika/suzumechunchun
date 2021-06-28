package testhelpers

// REF: https://github.com/sake92/PlayGuiceExample/blob/master/test/dao/PersonDAOImplSpec.scala#L17


trait TestDatabaseConfiguration {
  this: TestCase =>

  // Database tables will be migrated at testing (evolutions migration will be executed at testing).
  // Not necessary to migrate manually
  val slickProfileKey = "slick.dbs.default.profile"
  val slickDriverKey = "slick.dbs.default.db.driver"
  val slickUrlKey = "slick.dbs.default.db.url"

  val testDbProfileKey = "test.db.profile"
  val testDbDriverKey = "test.db.driver"
  val testDbUrl = "test.db.url"


  override def setAppConfiguration() = {
    Map(
      slickProfileKey -> conf.getString(testDbProfileKey),
      slickDriverKey -> conf.getString(testDbDriverKey),
      slickUrlKey -> conf.getString(testDbUrl)
    )
  }

}
