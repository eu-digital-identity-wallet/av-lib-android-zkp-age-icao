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

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import eu.europa.ec.av.zkp.icao.app.ui.screens.InputScreen
import eu.europa.ec.av.zkp.icao.app.ui.screens.nfc.NfcReaderScreen
import eu.europa.ec.av.zkp.icao.app.ui.screens.ZKPGenerationScreen
import eu.europa.ec.av.zkp.icao.app.ui.screens.nfc.NfcViewModel
import eu.europa.ec.av.zkp.icao.app.ui.theme.ICAODataTransformerTheme
import eu.europa.ec.av.zkp.icao.app.util.NFCHelper

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ICAODataTransformerTheme {
                val navController = rememberNavController()
                val nfcViewModel: NfcViewModel = viewModel()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "input",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("input") {
                            InputScreen { doc, dob, doe ->
                                navController.navigate("nfc/$doc/$dob/$doe")
                            }
                        }

                        composable(
                            "nfc/{doc}/{dob}/{doe}",
                            arguments = listOf(
                                navArgument("doc") { type = NavType.StringType },
                                navArgument("dob") { type = NavType.StringType },
                                navArgument("doe") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val doc = backStackEntry.arguments?.getString("doc") ?: ""
                            val dob = backStackEntry.arguments?.getString("dob") ?: ""
                            val doe = backStackEntry.arguments?.getString("doe") ?: ""

                            LaunchedEffect(Unit) {
                                nfcViewModel.clearIsoDep()
                            }

                            NfcReaderScreen(
                                documentNumber = doc,
                                dateOfBirth = dob,
                                dateOfExpiry = doe,
                                nfcViewModel = nfcViewModel,
                                onReadComplete = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "You can now remove the passport or ID card from the device",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("zkp") {
                                        popUpTo("nfc/{doc}/{dob}/{doe}") { inclusive = true }
                                    }
                                },
                                onReadError = {
                                    navController.popBackStack("input", false)
                                }
                            )
                        }

                        composable("zkp") {
                            ZKPGenerationScreen(nfcViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        isoDep?.let { isd ->
            runOnUiThread {
                val nfcViewModel: NfcViewModel = ViewModelProvider(this)[NfcViewModel::class.java]
                nfcViewModel.updateIsoDep(isd)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NFCHelper(this).enableReaderMode(this)
    }

    override fun onPause() {
        super.onPause()
        NFCHelper(this).disableReaderMode()
    }
}