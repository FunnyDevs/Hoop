package app.hoop.domain.identity

import app.hoop.domain.post.PostId

enum class NotificationKind { RepliesReceived, VideoPublished, QuestionPublished }

/** Something that happened around the user and links to a post. */
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val actorName: String,
    val postId: PostId,
    val count: Int,
    val distanceMeters: Int,
)
