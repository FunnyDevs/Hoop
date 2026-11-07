package app.hoop.domain.identity

import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

/** Entity: its identity is [id], not the values of its attributes. */
class User(
    val id: UserId,
    val username: Username,
    val email: Email,
    val displayName: String,
) {
    override fun equals(other: Any?): Boolean = other is User && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
