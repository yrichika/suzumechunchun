package helpers

import play.api.http.HttpErrorHandler
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.Singleton


@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(
        views.html.pages.error.e400(statusCode, message)(request)
      )
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      Status(INTERNAL_SERVER_ERROR)(
        views.html.pages.error.e500(INTERNAL_SERVER_ERROR, exception.getMessage)(request)
      )
    )
  }
}
