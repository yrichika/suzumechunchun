package testhelpers


import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OneInstancePerTest, Outcome}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.reflect.ClassTag
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
 * Test will be executed to any class which extends PlaySpec.
 * made this as trait so that this class will not run as a test
 */
trait TestCase extends PlaySpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
  with BeforeAndAfterAll
{

  /**
   * use this to get data from conf/application.conf
   */
  lazy val conf = ConfigFactory.load()


  // Can't use it as `super.app` because parent `app` is also lazy val.
  // implicit override lazy val app: Application = if (setAppConfiguration().isEmpty) super.app else overrideApp()
  implicit override lazy val app: Application = if (setAppConfiguration().isEmpty) fakeApplication() else overrideApp

  def getRequest(route: String) = FakeRequest(GET, route)

  def postRequest(route: String, input: String) = {
    FakeRequest(POST, route).withJsonBody(Json.parse(input))
  }

  /**
   * override it to configure testing application
   * @return
   */
  def setAppConfiguration(): Map[String, String] = {
    Map[String, String]()
  }


  def overrideApp = new GuiceApplicationBuilder().configure(setAppConfiguration()).build()


  def injectDependency[T: ClassTag](implicit app: Application): T = Application.instanceCache[T].apply(app)

}
