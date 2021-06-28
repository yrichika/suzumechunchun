package database.models

final case class CreatedChannel(
  hostChannelToken: String,
  loginChannelToken: String,
  secretKey: String
)
