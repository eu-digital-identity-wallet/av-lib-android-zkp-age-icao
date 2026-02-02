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
package eu.europa.ec.av.zkp.icao.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import eu.europa.ec.av.zkp.icao.app.util.NFCHelper

@Composable
fun InputScreen(onProceed: (String, String, String) -> Unit) {
    var documentNumber by remember { mutableStateOf(TextFieldValue("")) }
    var dateOfBirth by remember { mutableStateOf(TextFieldValue("")) }
    var dateOfExpiry by remember { mutableStateOf(TextFieldValue("")) }
    val activity = LocalActivity.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Please fill in your passport or ID card data:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = documentNumber,
            onValueChange = {
                val filtered = it.text.filter { c -> c.isLetterOrDigit() && c.code < 128 }
                documentNumber = TextFieldValue(filtered, it.selection)
            },
            label = { Text("Document Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = {
                val filtered = it.text.filter { c -> c.isDigit() }
                dateOfBirth = TextFieldValue(filtered, it.selection)
            },
            label = { Text("Date of Birth (yymmdd)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateOfExpiry,
            onValueChange = {
                val filtered = it.text.filter { c -> c.isDigit() }
                dateOfExpiry = TextFieldValue(filtered, it.selection)
            },
            label = { Text("Date of Expiry (yymmdd)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                keyboardController?.hide()
                if (activity != null && !NFCHelper(activity).isNfcEnabled()) {
                    Toast.makeText(activity, "Enable NFC", Toast.LENGTH_LONG).show()
                    return@Button
                }
                onProceed(documentNumber.text, dateOfBirth.text, dateOfExpiry.text)
            },
            enabled = documentNumber.text.isNotBlank() &&
                    dateOfBirth.text.length == 6 &&
                    dateOfExpiry.text.length == 6
        ) {
            Text("Proceed")
        }
    }
}