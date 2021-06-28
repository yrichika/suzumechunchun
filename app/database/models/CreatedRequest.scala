package database.models

final case class CreatedRequest(
  requestClientId: String,
  codename: String,
  passphrase: String,
  isAuthenticated: Option[Boolean]
)
