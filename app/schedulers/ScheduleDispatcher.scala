package schedulers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

/**
 * https://www.playframework.com/documentation/2.8.x/ScheduledTasks
 *
 * Need own dispatcher if use database.
 */
class ScheduleDispatcher @Inject() (actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "scheduleDispatcher") {

}
