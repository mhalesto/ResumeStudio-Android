package com.resumestudio.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Base64

/**
 * Bytes as a base64 string, which is how Swift's `JSONEncoder` writes `Data` by
 * default — so a photo saved on an iPhone opens here, and vice versa.
 *
 * Decoding is deliberately forgiving: a portrait that will not parse costs the
 * user a photo, whereas throwing costs them the whole résumé.
 */
object Base64ByteArraySerializer : KSerializer<ByteArray> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray =
        runCatching { Base64.getDecoder().decode(decoder.decodeString()) }
            .getOrElse { ByteArray(0) }
}
