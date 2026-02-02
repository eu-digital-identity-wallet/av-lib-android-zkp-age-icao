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
package eu.europa.ec.av.zkp.icao.app.ui.screens.nfc

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.europa.ec.av.zkp.icao.app.IcaoNfcReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jmrtd.BACKey

@Composable
fun NfcReaderScreen(
    documentNumber: String,
    dateOfBirth: String,
    dateOfExpiry: String,
    nfcViewModel: NfcViewModel,
    onReadComplete: () -> Unit,
    onReadError: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(nfcViewModel.getIsoDep()) {
        val isoDep = nfcViewModel.getIsoDep()
        if (isoDep == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val zkpIcaoData = withContext(Dispatchers.IO) {
            try {
                isoDep.timeout = 30000
                IcaoNfcReader().performReadTask(
                    isoDep = isoDep,
                    bacKey = BACKey(documentNumber, dateOfBirth, dateOfExpiry)
                )
            } catch (_: Exception) {
                null
            }
        }
        nfcViewModel.zkpIcaoData = zkpIcaoData
        loading = false
        nfcViewModel.clearIsoDep()
        withContext(Dispatchers.Main) {
            if (zkpIcaoData == null) {
                Toast.makeText(context, "Failed to read NFC data", Toast.LENGTH_LONG).show()
                onReadError()
            } else {
                onReadComplete()
            }
        }
    }
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Reading NFC passport or ID card data...")
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap your NFC passport or ID card to the device to read data.")
        }
    }
}