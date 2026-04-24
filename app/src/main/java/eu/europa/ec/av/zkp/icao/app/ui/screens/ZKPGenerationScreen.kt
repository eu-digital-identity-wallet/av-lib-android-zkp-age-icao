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

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import eu.europa.ec.av.zkp.icao.app.ui.screens.nfc.NfcViewModel
import eu.europa.ec.av.zkp.icao.app.util.Log
import eu.europa.ec.av.zkp.icao.app.util.zkpLogger
import eu.europa.ec.av.zkp.icao.ExperimentalZkpIcaoApi
import eu.europa.ec.av.zkp.icao.ZkpIcao
import eu.europa.ec.av.zkp.icao.buildAgeAttestations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalZkpIcaoApi::class)
@Composable
fun ZKPGenerationScreen(nfcViewModel: NfcViewModel) {
    val ageThresholds = listOf(18, 21, 30, 65)
    var loading by remember { mutableStateOf(true) }
    var proof by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val zkpIcao = ZkpIcao(
            context = context,
            logger = zkpLogger
        )
        val zkpIcaoData = nfcViewModel.zkpIcaoData
        if (zkpIcaoData != null) {
            val ageAttestations = zkpIcaoData.buildAgeAttestations(ageThresholds)
            val result = withContext(Dispatchers.IO) { zkpIcao.prove(zkpIcaoData, ageAttestations) }
            result.onSuccess { zkpProofResult ->
                proof = zkpProofResult.toJson()
            }.onFailure {
                proof = null
                Log.e("ZKPGenerationScreen", "Failed to generate ZKP proof", it)
            }
        } else {
            proof = null
        }
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Generating ZKP proof...")
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Proof: ${if (proof != null) "Generated successfully" else "Generated failed"}")
                Text("For age thresholds: ${ageThresholds.joinToString(", ")}")
                Spacer(modifier = Modifier.height(16.dp))
                if (proof != null) {
                    Button(
                        onClick = {
                            val proofText = proof ?: return@Button

                            // Write proof to a temp file
                            val file = File(context.cacheDir, "zkp-proof.txt").apply {
                                writeText(proofText, Charsets.UTF_8)
                            }

                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData =
                                    ClipData.newUri(context.contentResolver, "zkp-proof", uri)
                            }

                            val chooser =
                                Intent.createChooser(sendIntent, "Share ZKP Proof").apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                            context.startActivity(chooser)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Share ZKP")
                    }
                }
            }
        }
    }
}