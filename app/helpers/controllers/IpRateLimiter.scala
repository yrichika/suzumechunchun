package helpers.controllers

import com.digitaltangible.playguard.{IpRateLimitFilter, RateLimiter}
import play.api.mvc.Results.TooManyRequests
import play.api.mvc.{MessagesRequest, MessagesRequestHeader}

import scala.concurrent.{ExecutionContext, Future}

object IpRateLimiter {

  def throttle(size: Int, rate: Float)(implicit executionContext: ExecutionContext) = IpRateLimitFilter[MessagesRequest](
    new RateLimiter(size, rate, "Throttled: "), { requestHeader: MessagesRequestHeader =>
      Future.successful(TooManyRequests(s"""too many requests"""))
    }
  )
}
