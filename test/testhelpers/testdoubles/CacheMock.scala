package testhelpers.testdoubles

import akka.Done
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class CacheMock(implicit executionContext: ExecutionContext) extends AsyncCacheApi() {
  val cache = scala.collection.mutable.Map[String, Any]()

  def set(key: String, value: Any, expiration: Duration): Future[Done] = Future {
    cache.put(key, value)
    Done
  }

  def remove(key: String): Future[Done] = Future {
    cache -= key
    Done
  }

  def get[T: ClassTag](key: String): Future[Option[T]] = Future {
    cache.get(key).asInstanceOf[Option[T]]
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    get[A](key).flatMap {
      case Some(value) => Future.successful(value)
      case None => orElse.flatMap(value => set(key, value, expiration).map(_ => value))
    }
  }

  def removeAll(): Future[Done] = Future {
    cache.clear()
    Done
  }
}
