package app.hoop.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads a date as its ISO 8601 text. Parse sends a date either as plain text or, inside the answer of a
 * Cloud Function, as `{"__type":"Date","iso":"..."}`; both forms are accepted.
 */
internal object ParseDateAsIso : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ParseDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when (element) {
            is JsonObject -> element.getValue(ISO_KEY).jsonPrimitive.content
            is JsonPrimitive -> element.content
            else -> error("Not a date: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    private const val ISO_KEY = "iso"
}
