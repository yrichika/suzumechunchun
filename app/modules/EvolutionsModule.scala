package modules


import play.api.inject.SimpleModule
import play.api.db.evolutions._
import play.api.inject._


class EvolutionsModule extends SimpleModule(
  bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
  bind[EvolutionsReader].to[CustomEvolutionsReader],
  bind[EvolutionsApi].to[DefaultEvolutionsApi],
  bind[ApplicationEvolutions].toProvider[ApplicationEvolutionsProvider].eagerly()
)


