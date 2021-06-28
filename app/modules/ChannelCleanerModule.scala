package modules

import play.api.inject.SimpleModule
import play.api.inject._
import schedulers.ChannelCleaner

class ChannelCleanerModule extends SimpleModule(
  bind[ChannelCleaner].toSelf.eagerly()
)

