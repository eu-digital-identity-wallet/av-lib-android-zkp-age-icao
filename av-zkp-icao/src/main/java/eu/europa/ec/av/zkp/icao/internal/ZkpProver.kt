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
package eu.europa.ec.av.zkp.icao.internal

import com.noirandroid.lib.Circuit
import eu.europa.ec.av.zkp.icao.ZkpLogger
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.ExperimentalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * ZKP Prover using Noir Android library.
 *
 * @param circuitJson The JSON manifest of the ZKP circuit.
 * @param srsPath Optional path to a local SRS file. If not provided, the SRS will be downloaded
 * @param logger Optional logger to log debug and error messages. If not provided, no logging will be done.
 * from Aztec's server.
 */
internal class ZkpProver(circuitJson: String, srsPath: String? = null, val logger: ZkpLogger? = null) {

    private val circuit: Circuit by lazy {
        Circuit.fromJsonManifest(circuitJson).apply {
            logger?.d(TAG, "Using SRS path: $srsPath")
            setupSrs(srsPath)
        }
    }

    /**
     * Builds inputs from inputJson and generates a proof.
     * @param inputJson The passport / ID card data in JSON format.
     * @param ageAttestations Age attestation rules where key is the age threshold (1–99)
     * @return Result containing ProveResult on success.
     */
    fun prove(inputJson: String, ageAttestations: Map<Int, Boolean>): Result<String> = runCatching {
        val inputs = buildInputs(inputJson, ageAttestations)

        val start = System.nanoTime()
        val proof = circuit.prove(inputs)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        logger?.d(TAG, "Proof generated in ${"%.1f".format(elapsedMs)} ms")

        proof
    }.onFailure { e ->
        logger?.e(TAG, "Failed to prove circuit", e)
    }

    /**
     * Verifies an existing proof.
     * @param proofBase64 The proof in Base64 format.
     * @return Result containing a Boolean indicating if the proof is valid.
     */
    fun verify(proofBase64: String): Result<Boolean> = runCatching {
        val isValid = circuit.verify(proofBase64)
        logger?.d(TAG, "Proof verification: $isValid")
        isValid
    }.onFailure { e ->
        logger?.e(TAG, "Failed to verify proof", e)
    }


    @OptIn(ExperimentalTime::class)
    private fun buildInputs(inputJson: String, ageAttestations: Map<Int, Boolean>): HashMap<String, Any> {
        val jsonObj = JSONObject(inputJson)

        val dsc = jsonObj.getString("dsc")
        val id = jsonObj.getString("id")

        logger?.d(TAG, "Example DSC: $dsc")
        logger?.d(TAG, "Example ID: $id")

        require(dsc.isNotEmpty() && id.isNotEmpty()) {
            "DSC or ID is null/empty"
        }

        val dscJson = JSONObject(dsc)
        val idJson = JSONObject(id)

        val cscPubkey = dscJson.getJSONArray("csc_pubkey").toDoubleList()
        val dscSignature = dscJson.getJSONArray("dsc_signature").toDoubleList()
        val cscPubkeyRedcParam =
            dscJson.getJSONArray("csc_pubkey_redc_param").toDoubleList()

        val dscPubkey = idJson.getJSONArray("dsc_pubkey").toDoubleList()
        val dscPubkeyRedcParam =
            idJson.getJSONArray("dsc_pubkey_redc_param").toDoubleList()
        val sodSignature = idJson.getJSONArray("sod_signature").toDoubleList()

        val dg1Bytes = idJson.getJSONArray("dg1").toDoubleList()

        val signedAttributes = idJson.getJSONArray("signed_attributes").toDoubleList()
        val eContent = idJson.getJSONArray("e_content").toDoubleList()
        val tbsCertificate = idJson.getJSONArray("tbs_certificate").toDoubleList()

        val exponent = 65537
        val hexExponent = "0x" + exponent.toString(16)

        // Current date rounded to the hour in UTC, e.g., 2024-01-01T12:00:00Z
        val currentDate: ZonedDateTime =
            ZonedDateTime.now(ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val expectedCurrentDate = currentDate.toEpochSecond()
        val nowHex = "0x" + expectedCurrentDate.toString(16)

        val certificateRegistryRoot = dscJson.getString("certificate_registry_root")
        val certificateRegistryIndex =
            "0x" + dscJson.getInt("certificate_registry_index").toString(16)
        val certificateRegistryHashPath =
            dscJson.getJSONArray("certificate_registry_hash_path").toStringList()
        val certificateTags =
            dscJson.getJSONArray("certificate_tags").toStringList()
        val certificateType = dscJson.getString("certificate_type")
        val country = dscJson.getString("country")
        val salt = dscJson.getString("salt")

        val ruleAges = ageAttestations.keys.map { it.toDouble() }
            .padEnd(MAX_RULES, 0.0)
        val ruleOps = ageAttestations.values.map { isOver -> if (isOver) 1.0 else 2.0 }
            .padEnd(MAX_RULES, 0.0)

        val rulesLen = ageAttestations.size
        val hexRulesLen = "0x" + rulesLen.toString(16)

        return hashMapOf(
            "csc_pubkey" to cscPubkey,
            "dsc_signature" to dscSignature,
            "csc_pubkey_redc_param" to cscPubkeyRedcParam,
            "dsc_pubkey" to dscPubkey,
            "dsc_pubkey_redc_param" to dscPubkeyRedcParam,
            "sod_signature" to sodSignature,
            "dg1" to dg1Bytes,
            "signed_attributes" to signedAttributes,
            "e_content" to eContent,
            "tbs_certificate" to tbsCertificate,

            "exponent" to hexExponent,
            "current_date" to nowHex,

            "certificate_registry_root" to certificateRegistryRoot,
            "certificate_registry_index" to certificateRegistryIndex,
            "certificate_registry_hash_path" to certificateRegistryHashPath,
            "certificate_tags" to certificateTags,
            "certificate_type" to certificateType,
            "country" to country,
            "salt" to salt,
            "rule_ages" to ruleAges,
            "rule_ops" to ruleOps,
            "rules_len" to hexRulesLen
        )
    }

    private fun JSONArray.toDoubleList(): List<Double> =
        (0 until length()).map { i -> getDouble(i) }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { i -> getString(i) }

    private fun List<Double>.padEnd(size: Int, padValue: Double): List<Double> =
        this + List(size - this.size) { padValue }

    companion object Companion {
        private const val TAG = "ZkpProver"
        private const val MAX_RULES = 8
    }
}