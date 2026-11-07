package app.hoop.domain.hoop

import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.post.PostType
import kotlin.jvm.JvmInline

@JvmInline
value class HoopId(val value: String)

/**
 * Entity: an area of interest of the user.
 * The rule "what can be published where" lives here, not only in the UI or the server.
 */
sealed class Hoop {
    abstract val id: HoopId
    abstract val radius: Radius
    abstract val acceptedPostTypes: Set<PostType>

    fun accepts(type: PostType): Boolean = type in acceptedPostTypes

    /** Initial Hoop ("Home"): follows the current position, accepts every post type. */
    class Home(override val id: HoopId, override val radius: Radius) : Hoop() {
        override val acceptedPostTypes: Set<PostType> = PostType.entries.toSet()
        fun withRadius(radius: Radius) = Home(id, radius)
    }

    /** Additional Hoop chosen on the map: accepts questions only. */
    class Extra(
        override val id: HoopId,
        val name: String,
        val center: GeoPoint,
        override val radius: Radius,
    ) : Hoop() {
        override val acceptedPostTypes: Set<PostType> = setOf(PostType.Question)
        fun withRadius(radius: Radius) = Extra(id, name, center, radius)
        fun movedTo(center: GeoPoint) = Extra(id, name, center, radius)
    }

    override fun equals(other: Any?): Boolean = other is Hoop && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
