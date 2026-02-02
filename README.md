# Age Verification ZKP ICAO for Android

This project is an **experimental Android library** for age verification using **zero-knowledge proofs (ZKPs)**
derived from **ICAO-compliant ePassports and ID cards**.

## Dependencies
Add the library to your `build.gradle` file:

```groovy
dependencies {
    // TODO()
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

// Generate the zero-knowledge proof
val proofResult = zkpIcao.prove(zkpData)
proofResult
    .onSuccess { proof ->
        // Handle successful proof generation
    }
    .onFailure { error ->
        // Handle proof generation errors
    }
```

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