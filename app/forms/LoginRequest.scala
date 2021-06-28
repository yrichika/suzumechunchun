package forms

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}

final case class LoginRequest(codename: String, passphrase: String)

object LoginRequest {
  val form: Form[LoginRequest] = Form(
    mapping(
      "codename" -> nonEmptyText(1, 64),
      "passphrase" -> nonEmptyText(1, 128)
    )(LoginRequest.apply)(LoginRequest.unapply)
  )
}
