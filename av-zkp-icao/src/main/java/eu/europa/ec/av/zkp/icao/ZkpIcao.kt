/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.europa.ec.av.zkp.icao

import android.content.Context
import eu.europa.ec.av.zkp.icao.internal.ZkpJsEngine
import eu.europa.ec.av.zkp.icao.internal.ZkpProver
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayInputStream
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.COMFile
import java.security.MessageDigest
import java.util.Base64

/**
 * Opt-in annotation for experimental ZkpIcao API.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "ZkpIcao API is an experimental feature."
)

@Retention(AnnotationRetention.BINARY)

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)

annotation class ExperimentalZkpIcaoApi

/**
 * ZKP ICAO prover and verifier.
 * Opt-in using [ExperimentalZkpIcaoApi]. This is an experimental feature.
 *
 * @param context Android context
 * @param srsPath Optionally specify the path a local SRS file. If not provided, the SRS will be downloaded
 * @param logger Optional logger to log debug and error messages. If not provided, no logging will be done.
 * from Aztec's server.
 */
@ExperimentalZkpIcaoApi
class ZkpIcao(context: Context, srsPath: String? = null, val logger: ZkpLogger? = null) {

    private val zkpProver: ZkpProver =
        ZkpProver(
            circuitJson = context.assets.open("circuit.json").bufferedReader()
                .use { it.readText() },
            srsPath = srsPath,
            logger = logger
        )

    private val zkpJsEngine: ZkpJsEngine by lazy { ZkpJsEngine(context) }

    /**
     * Generates a zero-knowledge proof for the given passport/ID card data and age attestations.
     *
     * @param zkpIcaoData The passport/ID card data read via NFC.
     * @param ageAttestations Age attestation rules where key is the age threshold (1–99)
     *  and value is `true` to attest "is equal to or larger than that age" or `false` to attest "is smaller than that age".
     *  Maximum 8 entries.
     *  Example: `mapOf(18 to true, 21 to true, 65 to false)` attests is equal to or larger than 18,
     *  equal to or larger than 21, smaller than 65.
     *  @return Result containing ZkpProofResult on success, or error message on failure.
     */
    suspend fun prove(zkpIcaoData: ZkpIcaoData, ageAttestations: Map<Int, Boolean>): Result<ZkpProofResult> {

        // Validate input data
        zkpIcaoData.validate().onFailure { error ->
            logger?.e("ZkpIcao", "Invalid ZkpIcaoData: ${error.message}", error)
            return Result.failure(error)
        }

        // Validate age attestations
        validateAgeAttestations(ageAttestations).onFailure { error ->
            logger?.e("ZkpIcao", "Invalid age attestations: ${error.message}", error)
            return Result.failure(error)
        }

        return try {
            val inputJson = buildJson(zkpIcaoData)
            logger?.d("ZkpIcao", "Input JSON for ZKP circuit: $inputJson")
            zkpJsEngine.init()
            val resultJson = zkpJsEngine.generateArtifacts(inputJson.toString())
            zkpProver.prove(resultJson, ageAttestations).map { proof ->
                val data = ageAttestations.entries.associate { (age, isOver) ->
                    "age_over_${age.toString().padStart(2, '0')}" to isOver
                }
                ZkpProofResult(ageAttestations = data, proof = proof)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verify(proofBase64: String): Result<Boolean> {
        return zkpProver.verify(proofBase64)
    }
}

/**
 *  Builds the input JSON for the ZKP circuit from the passport or ID card data
*/
private fun buildJson(
    zkpIcaoData: ZkpIcaoData
): JsonObject {

    // --- Parse SOD from bytes ---
    val sodBytes = zkpIcaoData.sodFile
    val sod = sodBytes.toInputStream().use { SODFile(it) }

    // --- Parse COM from bytes ---
    val comBytes = zkpIcaoData.comFile
    val comFile = comBytes.toInputStream().use { COMFile(it) }

    // --- Parse DG1 from bytes ---
    val dg1Bytes = zkpIcaoData.dgFiles.getValue(DataGroupNumber(1))
    val dg1File = dg1Bytes.toInputStream().use { DG1File(it) }
    val mrzInfo = dg1File.mrzInfo

    // --- Calculate hash ---
    val dg1Hash = calculateHash(dg1Bytes, sod.digestAlgorithm.toString())

    return buildJsonObject {
        put("mrz", JsonPrimitive(mrzInfo.toString().replace("\n", "")))

        put("name", JsonPrimitive(mrzInfo.secondaryIdentifier + " " + mrzInfo.primaryIdentifier))
        put("dateOfBirth", JsonPrimitive(mrzInfo.dateOfBirth))
        put("nationality", JsonPrimitive(mrzInfo.nationality))
        put("gender", JsonPrimitive(mrzInfo.genderCode.toShort()))
        put("passportNumber", JsonPrimitive(mrzInfo.documentNumber))
        put("passportExpiry", JsonPrimitive(mrzInfo.dateOfExpiry))
        put("firstName", JsonPrimitive(mrzInfo.secondaryIdentifier))
        put("lastName", JsonPrimitive(mrzInfo.primaryIdentifier))
        put("fullName", JsonPrimitive(mrzInfo.secondaryIdentifier + " " + mrzInfo.primaryIdentifier))

        put("LDSVersion", JsonPrimitive(comFile.ldsVersion))

        // Data Groups, only DG1 for now
        put("dataGroups", buildJsonArray {
            // DG1 object
            addJsonObject {
                put("groupNumber", JsonPrimitive(1))
                put("name", JsonPrimitive("DG1"))

                put("hash", buildJsonArray {
                    dg1Hash.forEach { add(JsonPrimitive(it.toInt())) }
                })

                put("value", buildJsonArray {
                    dg1Bytes.forEach { add(JsonPrimitive(it.toInt())) }
                })
            }
        })

        put("dataGroupsHashAlgorithm", JsonPrimitive(sod.digestAlgorithm.toString()))

        put(
            "sod",
            buildJsonObject {
                put(
                    "encoded",
                    JsonPrimitive(Base64.getEncoder().encodeToString(sod.encoded))
                )
            }
        )
    }
}

private fun Gender.toShort(): String {
    return when (this.toString()) {
        "FEMALE" -> "F"
        "MALE" -> "M"
        else -> "X"
    }
}

private fun calculateHash(data: ByteArray, algorithm: String = "SHA-256"): ByteArray {
    val digest = MessageDigest.getInstance(algorithm)
    val hashBytes = digest.digest(data)
    return hashBytes
}

private fun ByteArray.toInputStream(): ByteArrayInputStream {
    return ByteArrayInputStream(this)
}

@JvmInline
value class DataGroupNumber(val value: Int)

data class ZkpIcaoData(
    val dgFiles: Map<DataGroupNumber, ByteArray>,
    val sodFile: ByteArray,
    val comFile: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZkpIcaoData

        if (dgFiles != other.dgFiles) return false
        if (!sodFile.contentEquals(other.sodFile)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dgFiles.hashCode()
        result = 31 * result + sodFile.contentHashCode()
        return result
    }
}

/**
 * Result of a ZKP proof generation, containing the age attestation claims and the proof.
 *
 * @param ageAttestations The age attestation claims, e.g. `{"age_over_18": true, "age_over_65": false}`.
 * @param proof The generated proof in Base64 format.
 */
@Serializable
data class ZkpProofResult(
    @SerialName("data")
    val ageAttestations: Map<String, Boolean>,
    val proof: String
) {
    fun toJson(): String = Json.encodeToString(this)
}

private fun validateAgeAttestations(ageAttestations: Map<Int, Boolean>): Result<Unit> {
    if (ageAttestations.isEmpty()) {
        return Result.failure(IllegalArgumentException("Age attestations must not be empty"))
    }
    if (ageAttestations.size > 8) {
        return Result.failure(IllegalArgumentException("Age attestations must not exceed 8 entries"))
    }
    val invalidAges = ageAttestations.keys.filter { it !in 1..99 }
    if (invalidAges.isNotEmpty()) {
        return Result.failure(IllegalArgumentException("Age thresholds must be between 1 and 99, got: $invalidAges"))
    }
    return Result.success(Unit)
}

private fun ZkpIcaoData.validate(): Result<Unit> {
    if (sodFile.isEmpty()) {
        return Result.failure(IllegalArgumentException("SOD file is empty"))
    }
    if (comFile.isEmpty()) {
        return Result.failure(IllegalArgumentException("COM file is empty"))
    }
    if( dgFiles.isEmpty()) {
        return Result.failure(IllegalArgumentException("DG files are missing"))
    }
    val dg1Bytes = dgFiles[DataGroupNumber(1)]
        ?: return Result.failure(IllegalArgumentException("DG1 file is missing"))
    if (dg1Bytes.isEmpty()) {
        return Result.failure(IllegalArgumentException("DG1 file is empty"))
    }
    return Result.success(Unit)
}

interface ZkpLogger {
    fun d(tag: String, msg: String) {}
    fun i(tag: String, msg: String) {}
    fun w(tag: String, msg: String, tr: Throwable? = null) {}
    fun e(tag: String, msg: String, tr: Throwable? = null) {}
    fun v(tag: String, msg: String) {}
}