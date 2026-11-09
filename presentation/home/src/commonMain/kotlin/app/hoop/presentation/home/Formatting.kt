package app.hoop.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.hoop.designsystem.component.HoopPostKind
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val METERS_PER_KILOMETER = 1000
private const val TENTHS = 10
private const val METERS_PER_TENTH = 100.0
private const val MINUTES_PER_HOUR = 60
private const val DECIMAL_SEPARATOR = ','

/** Kilometers with at most one decimal and a comma separator (the app is Italian): 2500 → "2,5", 1000 → "1". */
internal fun formatKilometers(meters: Int): String {
    val tenths = (meters / METERS_PER_TENTH).roundToInt()
    return if (tenths % TENTHS == 0) "${tenths / TENTHS}" else "${tenths / TENTHS}$DECIMAL_SEPARATOR${tenths % TENTHS}"
}

@Composable
internal fun formatDistance(meters: Int): String =
    if (meters < METERS_PER_KILOMETER) stringResource(Res.string.distance_meters, meters)
    else stringResource(Res.string.distance_kilometers, formatKilometers(meters))

@Composable
internal fun formatAge(minutes: Int): String = when {
    minutes < 1 -> stringResource(Res.string.time_now)
    minutes < MINUTES_PER_HOUR -> stringResource(Res.string.time_minutes_ago, minutes)
    else -> stringResource(Res.string.time_hours_ago, minutes / MINUTES_PER_HOUR)
}

internal fun PostType.toKind(): HoopPostKind = when (this) {
    PostType.Question -> HoopPostKind.Question
    PostType.Photo -> HoopPostKind.Photo
    PostType.Video -> HoopPostKind.Video
}

@Composable
internal fun mediaPainter(key: String?): Painter? = when (key) {
    SampleMedia.SUNSET -> painterResource(Res.drawable.sample_sunset)
    SampleMedia.CONCERT -> painterResource(Res.drawable.sample_concert)
    SampleMedia.MARKET -> painterResource(Res.drawable.sample_market)
    else -> null
}

@Composable
internal fun tagLabel(type: PostType): String = stringResource(
    when (type) {
        PostType.Question -> Res.string.tag_question
        PostType.Photo -> Res.string.tag_photo
        PostType.Video -> Res.string.tag_video
    },
)

@Composable
internal fun repliesLabel(post: FeedPost): String =
    if (post.type == PostType.Question) pluralStringResource(Res.plurals.replies_count, post.replies.size, post.replies.size)
    else post.replies.size.toString()

@Composable
internal fun repliesHeading(post: FeedPost): String = pluralStringResource(
    if (post.type == PostType.Question) Res.plurals.replies_count else Res.plurals.comments_count, post.replies.size, post.replies.size,
)

internal fun initialsOf(name: String): String = name.take(2).uppercase()
