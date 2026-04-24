//[av-zkp-icao](../../index.md)/[eu.europa.ec.av.zkp.icao](index.md)/[buildAgeAttestations](build-age-attestations.md)

# buildAgeAttestations

[androidJvm]\
fun [ZkpIcaoData](-zkp-icao-data/index.md).[buildAgeAttestations](build-age-attestations.md)(ageThresholds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;, referenceDate: [LocalDate](https://developer.android.com/reference/kotlin/java/time/LocalDate.html) = LocalDate.now(ZoneOffset.UTC)): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;

Parses the date of birth from DG1 in this [ZkpIcaoData](-zkp-icao-data/index.md), calculates the holder's age, and returns a map of each threshold to whether the holder is >= that age.

#### Return

map where each key is a threshold and the value is `true` if the holder's age >= threshold.

#### Parameters

androidJvm

| | |
|---|---|
| ageThresholds | list of age thresholds (0–99) to check against. |
| referenceDate | the date to calculate the age against. Defaults to today's date in UTC. This should match the circuit's `current_date` reference for consistency. |
