package database.models

import java.sql.Timestamp

final case class AuthenticatedClient(
  id: Int,
  authenticatedClientIdHash: String,
  authenticatedClientIdEnc: Array[Byte],
  requestClientIdHash: String,
  channelId: String,
  createdAt: Timestamp,
  updatedAt: Timestamp
)
