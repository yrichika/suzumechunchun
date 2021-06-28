package database.models

import java.sql.Blob
import java.sql.Timestamp

final case class ChannelToken(
  id: Int,
  channelTokenId: String,
  channelId: String,
  channelNameEnc: Array[Byte],
  hostChannelTokenHash: String,
  loginChannelTokenHash: String,
  loginChannelTokenEnc: Array[Byte],
  clientChannelTokenHash: String,
  clientChannelTokenEnc: Array[Byte],
  secretKeyEnc: Option[Array[Byte]],
  createdAt: Timestamp,
  updatedAt: Timestamp
)
