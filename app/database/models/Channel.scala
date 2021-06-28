package database.models

import java.sql.Timestamp

final case class Channel(
  id: Int,
  channelId: String,
  hostIdHash: String,
  createdAt: Timestamp,
  updatedAt: Timestamp
)
