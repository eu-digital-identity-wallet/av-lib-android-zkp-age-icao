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

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle

/**
 * Helper class for managing NFC reader mode.
 */
internal class NFCHelper(private val activity: Activity) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    companion object {
        private const val TAG = "NFCHelper"

        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }

    fun isNfcEnabled(): Boolean =
        nfcAdapter?.isEnabled == true

    fun enableReaderMode(callback: NfcAdapter.ReaderCallback) {
        val adapter = nfcAdapter ?: return
        Log.i(TAG, "Enabling reader mode")
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)
        }
        adapter.enableReaderMode(activity, callback, READER_FLAGS, options)
    }

    fun disableReaderMode() {
        Log.i(TAG, "Disabling reader mode")
        nfcAdapter?.disableReaderMode(activity)
    }
}