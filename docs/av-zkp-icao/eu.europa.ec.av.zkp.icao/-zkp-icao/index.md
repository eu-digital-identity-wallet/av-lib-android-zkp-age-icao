//[av-zkp-icao](../../../index.md)/[eu.europa.ec.av.zkp.icao](../index.md)/[ZkpIcao](index.md)

# ZkpIcao

class [ZkpIcao](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), srsPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val logger: [ZkpLogger](../-zkp-logger/index.md)? = null)

ZKP ICAO prover and verifier. Opt-in using [ExperimentalZkpIcaoApi](../-experimental-zkp-icao-api/index.md). This is an experimental feature.

#### Parameters

androidJvm

| | |
|---|---|
| context | Android context |
| srsPath | Optionally specify the path a local SRS file. If not provided, the SRS will be downloaded |
| logger | Optional logger to log debug and error messages. If not provided, no logging will be done. from Aztec's server. |

## Constructors

| | |
|---|---|
| [ZkpIcao](-zkp-icao.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), srsPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, logger: [ZkpLogger](../-zkp-logger/index.md)? = null) |

## Properties

| Name | Summary |
|---|---|
| [logger](logger.md) | [androidJvm]<br>val [logger](logger.md): [ZkpLogger](../-zkp-logger/index.md)? = null |

## Functions

| Name | Summary |
|---|---|
| [prove](prove.md) | [androidJvm]<br>suspend fun [prove](prove.md)(zkpIcaoData: [ZkpIcaoData](../-zkp-icao-data/index.md)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt; |
| [verify](verify.md) | [androidJvm]<br>suspend fun [verify](verify.md)(proofBase64: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt; |
