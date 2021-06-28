package database.models

import java.sql.{Blob, Timestamp}

final case class ClientLoginRequest(
  id: Int,
  clientLoginRequestId: String,
  requestClientIdHash: String,
  requestClientIdEnc: Array[Byte],
  channelId: String,
  codenameEnc: Option[Array[Byte]],
  passphraseEnc: Option[Array[Byte]],
  isAuthenticated: Option[Boolean],
  createdAt: Timestamp,
  updatedAt: Timestamp
)
