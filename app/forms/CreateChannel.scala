package forms

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}

final case class CreateChannel(channelName: String)

object CreateChannel {
  val form: Form[CreateChannel] = Form(
    mapping(
      "channelName" -> nonEmptyText(1, 32)
    )(CreateChannel.apply)(CreateChannel.unapply)
  )
}
