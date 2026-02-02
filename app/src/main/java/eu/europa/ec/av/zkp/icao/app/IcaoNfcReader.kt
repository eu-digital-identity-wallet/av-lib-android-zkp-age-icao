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
package eu.europa.ec.av.zkp.icao.app

import android.nfc.tech.IsoDep
import eu.europa.ec.av.zkp.icao.app.util.Log
import eu.europa.ec.av.zkp.icao.app.util.PassportServiceExt.getComFile
import eu.europa.ec.av.zkp.icao.app.util.PassportServiceExt.getDG1File
import eu.europa.ec.av.zkp.icao.app.util.PassportServiceExt.getPaceInfos
import eu.europa.ec.av.zkp.icao.app.util.PassportServiceExt.getSodFile
import eu.europa.ec.av.zkp.icao.DataGroupNumber
import eu.europa.ec.av.zkp.icao.ZkpIcaoData
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKeySpec
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.PACEInfo
import java.security.GeneralSecurityException

/**
 * ICAO NFC Reader an example of how to read data from ePassports or ID cards using NFC.
 */
class IcaoNfcReader {

    fun performReadTask(isoDep: IsoDep, bacKey: BACKeySpec): ZkpIcaoData {
        Log.d(TAG, "Access started...")

        var ps: PassportService? = null
        try {
            val cs = CardService.getInstance(isoDep)
            ps = PassportService(
                cs,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                true
            )
            ps.open()

            val paceSucceeded = tryDoPace(ps, bacKey)

            ps.sendSelectApplet(paceSucceeded)

            if (!paceSucceeded) {
                // If PACE failed, do BAC.
                ps.doBAC(bacKey)
                Log.d(TAG, "BAC done")
            }

            Log.d(TAG, "Reading COM & SOD...")

            val comFile = ps.getComFile()
            Log.d(TAG, "COM read (${comFile.encoded.size} bytes)")

            val sodFile = ps.getSodFile()
            Log.d(TAG, "SOD read (${sodFile.encoded.size} bytes)")

            Log.d(TAG, "Reading DG1...")

            val (_, dg1Bytes) = ps.getDG1File()
            Log.d(TAG, "DG1 read (${dg1Bytes!!.size} bytes)")

            return ZkpIcaoData(
                dgFiles = mapOf(DataGroupNumber(1) to dg1Bytes),
                sodFile = sodFile.encoded,
                comFile = comFile.encoded
            )
        } catch (e: Exception) {
            Log.e(TAG, "Read failed", e)
            throw e
        } finally {
            try {
                ps?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close PassportService", e)
            }
        }
    }

    private fun tryDoPace(ps: PassportService, bacKey: BACKeySpec): Boolean {
        return try {
            val paceInfos = ps.getPaceInfos()
            if (paceInfos.isEmpty()) return false
            val paceKey = try {
                PACEKeySpec.createMRZKey(bacKey)
            } catch (e: GeneralSecurityException) {
                Log.e(TAG, "Failed to create MRZ PACE key", e)
                return false
            }
            for (paceInfo in paceInfos) {
                try {
                    ps.doPACE(
                        paceKey,
                        paceInfo.objectIdentifier,
                        PACEInfo.toParameterSpec(paceInfo.parameterId),
                        null
                    )
                    Log.d(TAG, "PACE succeeded")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "PACE attempt failed for OID=${paceInfo.objectIdentifier}", e)
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "PACE check failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "ICAONFCReader"
    }
}