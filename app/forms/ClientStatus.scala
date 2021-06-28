package forms

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, nonEmptyText}


final case class ClientStatus(requestClientId: String, status: Boolean)

object ClientStatus {
  val form: Form[ClientStatus] = Form(
    mapping(
      "requestClientId" -> nonEmptyText(1, 256),
      "status" -> boolean
    )(ClientStatus.apply)(ClientStatus.unapply)
  )
}

