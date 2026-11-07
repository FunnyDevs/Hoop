package app.hoop.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    val objectId: String,
    val username: String,
    val email: String,
    val name: String? = null,
    val sessionToken: String? = null,
)

@Serializable
internal data class FunctionResponse<T>(val result: T)

@Serializable
internal data class SignUpResultDto(val sessionToken: String, val user: UserDto)

@Serializable
internal data class AvailabilityDto(val usernameTaken: Boolean = false, val emailTaken: Boolean = false)

@Serializable
internal data class ParseErrorDto(val code: Int = 0, @SerialName("error") val message: String = "")

@Serializable
internal data class SignUpBody(
    val username: String,
    val email: String,
    val password: String,
    val radius: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
internal data class AvailabilityBody(val username: String? = null, val email: String? = null)

@Serializable
internal data class PasswordResetBody(val email: String)
