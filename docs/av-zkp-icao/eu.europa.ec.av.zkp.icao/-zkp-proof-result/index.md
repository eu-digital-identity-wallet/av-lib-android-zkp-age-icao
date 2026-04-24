//[av-zkp-icao](../../../index.md)/[eu.europa.ec.av.zkp.icao](../index.md)/[ZkpProofResult](index.md)

# ZkpProofResult

@Serializable

data class [ZkpProofResult](index.md)(val ageAttestations: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;, val proof: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Result of a ZKP proof generation, containing the age attestation claims and the proof.

#### Parameters

androidJvm

| | |
|---|---|
| ageAttestations | The age attestation claims, e.g. `{"age_over_18": true, "age_over_65": false}`. |
| proof | The generated proof in Base64 format. |

## Constructors

| | |
|---|---|
| [ZkpProofResult](-zkp-proof-result.md) | [androidJvm]<br>constructor(ageAttestations: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;, proof: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [ageAttestations](age-attestations.md) | [androidJvm]<br>@SerialName(value = &quot;data&quot;)<br>val [ageAttestations](age-attestations.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt; |
| [proof](proof.md) | [androidJvm]<br>val [proof](proof.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

## Functions

| Name | Summary |
|---|---|
| [toJson](to-json.md) | [androidJvm]<br>fun [toJson](to-json.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
