package helpers.controllers

import play.api.{Environment, Mode}

trait EnvironmentAware {
  val environment: Environment

  def isProd = environment.mode == Mode.Prod
  def isDev = environment.mode == Mode.Dev
  def isTest = environment.mode == Mode.Test
}
