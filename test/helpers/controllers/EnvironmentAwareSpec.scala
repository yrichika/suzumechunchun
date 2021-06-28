package helpers.controllers

import play.api.{Environment, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import testhelpers.{TestCase, TestDatabaseConfiguration}

import java.io.File

class EnvironmentAwareSpec extends TestCase with TestDatabaseConfiguration {


  "isTest" should {
    val testEnv = Environment.simple(mode = Mode.Test)
    "be true in Test Mode" in new EnvironmentAware {
      override implicit val environment: Environment = testEnv
      isTest mustBe true
    }
    "others should be false in Test Mode" in new EnvironmentAware {
      override implicit val environment: Environment = testEnv
      isProd mustBe false
      isDev mustBe false
    }
  }

  "isDev" should {
    val devEnv = Environment.simple(mode = Mode.Dev)
    "be true in Dev Mode" in new EnvironmentAware {
      override implicit val environment: Environment = devEnv
      isDev mustBe true
    }
    "others should be false in Dev Mode" in new EnvironmentAware {
      override implicit val environment: Environment = devEnv
      isProd mustBe false
      isTest mustBe false
    }
  }

  "isProd" should {
    val prodEnv = Environment.simple(mode = Mode.Prod)
    "be true in Prod Mode" in new EnvironmentAware {
      override implicit val environment: Environment = prodEnv
      isProd mustBe true
    }
    "others should be false in Prod Mode" in new EnvironmentAware {
      override implicit val environment: Environment = prodEnv
      isDev mustBe false
      isTest mustBe false
    }
  }
}
