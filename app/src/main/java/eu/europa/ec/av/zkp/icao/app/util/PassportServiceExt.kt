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
package eu.europa.ec.av.zkp.icao.app.util

import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.COMFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Extension functions for PassportService to read specific files.
 */
internal object PassportServiceExt {
    private const val TAG = "PassportServiceExt"

    fun PassportService.getCardAccessFile(maxBlockSize: Int = PassportService.DEFAULT_MAX_BLOCKSIZE): CardAccessFile {
        Log.d(TAG, "Read CardAccessFile started")
        var isCaFile: InputStream? = null
        try {
            isCaFile = this.getInputStream(PassportService.EF_CARD_ACCESS, maxBlockSize)
            val caFile = CardAccessFile(isCaFile)
            Log.d(TAG, "CardAccessFile: ${caFile.encoded.toHexString()}")
            return caFile
        } finally {
            isCaFile?.close()
        }
    }

    fun PassportService.getComFile(maxBlockSize: Int = PassportService.DEFAULT_MAX_BLOCKSIZE): COMFile {
        Log.d(TAG, "Read COMFile started")
        var isComFile: InputStream? = null
        try {
            isComFile = this.getInputStream(PassportService.EF_COM, maxBlockSize)
            val comFile = LDSFileUtil.getLDSFile(PassportService.EF_COM, isComFile) as COMFile
            Log.d(TAG, "COMFile: ${comFile.encoded.toHexString()}")
            return comFile
        } finally {
            isComFile?.close()
        }
    }

    fun PassportService.getSodFile(maxBlockSize: Int = PassportService.DEFAULT_MAX_BLOCKSIZE): SODFile {
        Log.d(TAG, "Read SODFile started")
        var isSodFile: InputStream? = null
        try {
            isSodFile = this.getInputStream(PassportService.EF_SOD, maxBlockSize)
            val sodFile = LDSFileUtil.getLDSFile(PassportService.EF_SOD, isSodFile) as SODFile
            Log.d(TAG, "SODFile: ${sodFile.encoded.toHexString()}")
            return sodFile
        } finally {
            isSodFile?.close()
        }
    }

    fun PassportService.getDG1File(maxBlockSize: Int = PassportService.DEFAULT_MAX_BLOCKSIZE): Pair<DG1File, ByteArray?> {
        Log.d(TAG, "Read DG1File started")
        var isDG1: InputStream? = null
        try {
            isDG1 = this.getInputStream(PassportService.EF_DG1, maxBlockSize)
            isDG1.mark(0)
            val dG1File = LDSFileUtil.getLDSFile(PassportService.EF_DG1, isDG1) as DG1File
            isDG1.reset()
            val dG1Bytes = isDG1.getBytes()
            Log.d(TAG, "DG1File (encoded): ${dG1File.encoded.toHexString()}")
            Log.d(TAG, "DG1File (bytes): ${dG1Bytes?.toHexString()}")
            return Pair(dG1File, dG1Bytes)
        } finally {
            isDG1?.close()
        }
    }

    fun PassportService.getPaceInfos(): List<PACEInfo> {
        val securityInfos = this.getCardAccessFile().securityInfos
        val paceInfos = mutableListOf<PACEInfo>()
        if (securityInfos == null) {
            return paceInfos
        }
        for (info in securityInfos) {
            if (info is PACEInfo) {
                paceInfos.add(info)
            }
        }
        return paceInfos
    }

    private fun InputStream.getBytes(): ByteArray? {
        var bytes: ByteArray? = null

        try {
            val bos = ByteArrayOutputStream()

            val data = ByteArray(1024)
            var count: Int

            while ((this.read(data).also { count = it }) != -1) {
                bos.write(data, 0, count)
            }

            bos.flush()
            bos.close()
            this.close()

            bytes = bos.toByteArray()
        } catch (e: IOException) {
            Log.e(TAG, "Error reading InputStream to bytes", e)
        }
        return bytes
    }
}