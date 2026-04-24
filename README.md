# Age Verification ZKP ICAO for Android

## Overview

This project is an **experimental Android library** for age verification using **zero-knowledge proofs (ZKPs)**
derived from **ICAO-compliant ePassports and ID cards**.

## Scope and Limitations

> **Note:** The current circuit implementation supports only a specific subset of ICAO-compliant ePassports and cryptographic configurations.

### Supported Cryptographic Parameters

The circuit currently assumes the following:

- **DSC certificate signature algorithm:**  
  RSA PKCS#1 v1.5 with SHA-256

- **SOD signature algorithm:**  
  RSASSA-PSS with SHA-256

- **CSC public key:**  
  RSA-4096

- **DSC public key:**  
  RSA-3072

- **TBSCertificate size:**  
  1600 bytes


## Disclaimer

The released software is an initial development release version:

- The initial development release is an early endeavor reflecting the efforts of a short timeboxed
  period, and by no means can be considered as the final product.
- The initial development release may be changed substantially over time, might introduce new
  features but also may change or remove existing ones, potentially breaking compatibility with your
  existing code.
- The initial development release is limited in functional scope.
- The initial development release may contain errors or design flaws and other problems that could
  cause system or other failures and data loss.
- The initial development release has reduced security, privacy, availability, and reliability
  standards relative to future releases. This could make the software slower, less reliable, or more
  vulnerable to attacks than mature software.
- The initial development release is not yet comprehensively documented.
- Users of the software must perform sufficient engineering and additional testing in order to
  properly evaluate their application and determine whether any of the open-sourced components is
  suitable for use in that application.
- We strongly recommend not putting this version of the software into production use.
- Only the latest version of the software will be supported

## Dependencies

To use **SNAPSHOT** versions, add the following repository to your project’s `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
  repositories {
    // ...
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      mavenContent { snapshotsOnly() }
    }
    // ...
  }
}
```

To include the library in your project, add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("eu.europa.ec.eudi:av-lib-android-zkp-age-icao:0.0.1-SNAPSHOT")
}
```

## Usage

In your Android project, you can use the library as follows:

```kotlin
// Prepare the ZKP ICAO data (DG1, SOD, and COM files)
// These are typically read from an NFC-enabled ePassport or ID card

val dg1Bytes: ByteArray = byteArrayOf(/* Read DG1 file bytes */)
val sodBytes: ByteArray = byteArrayOf(/* Read SOD file bytes */)
val comBytes: ByteArray = byteArrayOf(/* Read COM file bytes */)

val zkpData = ZkpIcaoData(
    dgFiles = mapOf(
        DataGroupNumber(1) to dg1Bytes
    ),
    sodFile = byteArrayOf(sodBytes),
    comFile = byteArrayOf(comBytes)
)

// Initialize the ZKP ICAO prover for Android
val zkpIcao = ZkpIcao(
    context = context,
    srsPath = null, // Optional: path to a local SRS file
    logger = null   // Optional: your own ZkpLogger implementation
)

// Define which age thresholds to attest and whether the holder claims to be over each one.
// Key = age threshold (0–99), Value = true for "is over or equal to", false for "is under".
// Up to 8 entries are supported.
val ageAttestations = mapOf(
    18 to true,   // claim: holder is 18 or older
    21 to true,   // claim: holder is 21 or older
    65 to false   // claim: holder is under 65
)

// Or, derive the booleans automatically from the holder's date of birth in DG1:
val ageAttestations = zkpData.buildAgeAttestations(listOf(18, 21, 65))

// Generate the zero-knowledge proof
val proofResult = zkpIcao.prove(zkpData, ageAttestations)
proofResult
    .onSuccess { zkpProofResult ->
        // zkpProofResult.ageAttestations -> {"age_over_18": true, "age_over_21": true, "age_over_65": false}
        // zkpProofResult.proof           -> Base64-encoded ZK proof
        val jsonPayload = zkpProofResult.toJson() // ready to send to a verifier
    }
    .onFailure { error ->
        // Handle proof generation errors
    }
```

### Age attestations

The library proves **age-over claims** in zero knowledge: the verifier learns whether
the holder's age satisfies each claim and not the actual date of birth.

- **`ageAttestations: Map<Int, Boolean>`** — passed to `prove()`. These are the claims the
  holder (or the verifier, via the holder's app) wants the proof to certify.
    - Key is the age threshold (`0..99`); value is the claim being made:
      `true` means "holder is **≥** that age", `false` means "holder is **<** that age".
    - Up to **8 claims** are supported per proof.
    - The ZK circuit verifies each claim against the holder's date of birth (extracted from
      DG1) and today's UTC date. If any claim is false for the actual data (e.g. the map
      says `18 to true` but the holder is 15), proof generation fails — you cannot forge a
      false claim.

- **`ZkpIcaoData.buildAgeAttestations(thresholds, referenceDate)`** — convenience helper that
  parses DG1, calculates the holder's age against `referenceDate` (defaults to today in UTC),
  and returns the `Map<Int, Boolean>` you pass to `prove()`. Use this when you want to
  automatically fill in truthful claims for a list of thresholds.

- **`ZkpProofResult`** — the success value returned by `prove()`:
    ```kotlin
    @Serializable
    data class ZkpProofResult(
        val ageAttestations: Map<String, Boolean>, // e.g. {"age_over_18": true, ...}
        val proof: String                          // Base64-encoded ZK proof
    ) {
        fun toJson(): String
    }
    ```
  `toJson()` serializes the result as:
    ```json
    {
      "data": { "age_over_18": true, "age_over_21": true, "age_over_65": false },
      "proof": "..."
    }
    ```
  This payload is ready to send to a verifier, which can check the proof against the
  claims.

### Structured Reference String (SRS)

The library requires a **Structured Reference String (SRS)** for proof generation.

You can either:
- Provide a **local SRS file path** when initializing `ZkpIcao`, or
- Allow the library to **download the SRS automatically** from Aztec’s server.

If you choose automatic download, add the following permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

A local copy of the SRS file is available in the project at:

```
av-zkp-icao/src/androidTest/res/raw/srs.local
```

## Sample App

A sample application demonstrating how to use the library is included in the project.  
It shows how to:

- Read data from an NFC-enabled ePassport or ID card
- Generate a zero-knowledge proof for age verification using this library

## How to contribute

We welcome contributions to this project. To ensure that the process is smooth for everyone
involved, follow the guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

### Third-party component licenses

See [licenses.md](licenses.md) for details.

### License details

Copyright (c) 2023 European Commission

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.