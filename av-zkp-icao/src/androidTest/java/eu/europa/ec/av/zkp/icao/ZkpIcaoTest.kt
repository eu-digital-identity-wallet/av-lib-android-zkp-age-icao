package eu.europa.ec.av.zkp.icao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import eu.europa.ec.av.zkp.icao.test.R
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ZkpIcaoTest {

    /**
     * Tests the prove and verify functions of ZkpIcao.
     */
    @OptIn(ExperimentalZkpIcaoApi::class)
    @Test
    @Ignore("Ignore this test by default as it requires sample data in resources.")
    fun proveAndVerify() = runBlocking {

        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create a test ZkpIcaoData
        val zkpIcaoData: ZkpIcaoData = createTestZkpIcaoData(context)

        val zkpIcao = ZkpIcao(
            context = context,
            srsPath = getLocalSrs(
                context,
                R.raw.srs
            ).absolutePath // Here we use a local SRS file for testing
        )

        // val referenceDate = LocalDate.now()
        val ageAttestations = zkpIcaoData.buildAgeAttestations(listOf(18, 21, 30, 65))

        // First, generate the proof
        val result = zkpIcao.prove(zkpIcaoData, ageAttestations)
        Assert.assertTrue(result.isSuccess)

        val zkpProofResult = result.getOrNull()
        Assert.assertNotNull("ZkpProofResult should not be null on success", zkpProofResult)
        Assert.assertTrue("Proof string should not be empty", zkpProofResult!!.proof.isNotEmpty())

        Assert.assertEquals("Should have 4 age attestation entries", 4, zkpProofResult.ageAttestations.size)

        Assert.assertTrue("Should contain age_over_18", zkpProofResult.ageAttestations.containsKey("age_over_18"))
        Assert.assertTrue("Should contain age_over_21", zkpProofResult.ageAttestations.containsKey("age_over_21"))
        Assert.assertTrue("Should contain age_over_30", zkpProofResult.ageAttestations.containsKey("age_over_30"))
        Assert.assertTrue("Should contain age_over_65", zkpProofResult.ageAttestations.containsKey("age_over_65"))

        // Now verify the proof
        val verification = zkpIcao.verify(zkpProofResult.proof)
        Assert.assertTrue(verification.isSuccess)
        val isValid = verification.getOrNull()
        Assert.assertNotNull("Verification result should not be null on success", isValid)
        Assert.assertTrue("Proof should be valid", isValid == true)
    }

    @OptIn(ExperimentalZkpIcaoApi::class)
    @Test
    fun invalidInputData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val zkpIcao = ZkpIcao(
            context = context,
            srsPath = getLocalSrs(context, R.raw.srs).absolutePath
        )

        // Test with empty ZkpPassportData
        val emptyData = ZkpIcaoData(
            dgFiles = emptyMap(),
            sodFile = ByteArray(0),
            comFile = ByteArray(0)
        )

        runBlocking {
            val result = zkpIcao.prove(emptyData, mapOf(18 to true))
            Assert.assertTrue("Proving should fail with empty data", result.isFailure)
        }
    }

    /**
     * Creates a test ZkpIcaoData by loading sample data from resources.
     */
    private fun createTestZkpIcaoData(context: Context): ZkpIcaoData {
        return ZkpIcaoData(
            dgFiles = mapOf(
                DataGroupNumber(1) to rawText(
                    context, R.raw.dg1
                ).hexToByteArray(HexFormat.Default),
            ),
            sodFile = rawText(context, R.raw.sod).hexToByteArray(HexFormat.Default),
            comFile = rawText(context, R.raw.com).hexToByteArray(HexFormat.Default)
        )
    }

    /**
     * Reads raw text from a resource file.
     */
    private fun rawText(context: Context, resId: Int): String =
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

    /**
     * Copies the local SRS file from resources to a no-backup directory and returns the file.
     */
    private fun getLocalSrs(context: Context, resId: Int): File {
        val outFile = File(context.noBackupFilesDir, "srs.local")
        context.resources.openRawResource(resId).use { input ->
            val tmp = File(outFile.parentFile, "srs.local.tmp")
            tmp.outputStream().use { output ->
                input.copyTo(output)
            }
            if (outFile.exists()) outFile.delete()
            tmp.renameTo(outFile)
        }
        return outFile
    }
}