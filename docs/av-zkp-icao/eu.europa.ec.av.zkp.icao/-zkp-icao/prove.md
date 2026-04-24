//[av-zkp-icao](../../../index.md)/[eu.europa.ec.av.zkp.icao](../index.md)/[ZkpIcao](index.md)/[prove](prove.md)

# prove

[androidJvm]\
suspend fun [prove](prove.md)(zkpIcaoData: [ZkpIcaoData](../-zkp-icao-data/index.md), ageAttestations: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[ZkpProofResult](../-zkp-proof-result/index.md)&gt;

Generates a zero-knowledge proof for the given passport/ID card data and age attestations.

#### Return

Result containing [ZkpProofResult](../-zkp-proof-result/index.md) on success, or error on failure.

#### Parameters

androidJvm

| | |
|---|---|
| zkpIcaoData | The passport/ID card data read via NFC. |
| ageAttestations | Age attestation claims where key is the age threshold (0–99) and value is `true` to claim &quot;is equal to or larger than that age&quot; or `false` to claim &quot;is smaller than that age&quot;. Maximum 8 entries. The ZK circuit verifies each claim against the actual date of birth — if any claim is false for the real data, proof generation fails.<br>Example: `mapOf(18 to true, 21 to true, 65 to false)` claims the holder is at least 18, at least 21, and under 65.<br>To derive these booleans automatically from the DG1 date of birth, use [ZkpIcaoData.buildAgeAttestations](../build-age-attestations.md). |
