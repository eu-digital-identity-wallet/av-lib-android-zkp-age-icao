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

import android.nfc.tech.IsoDep
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import eu.europa.ec.av.zkp.icao.ZkpIcaoData

class NfcViewModel : ViewModel() {
    private val _isoDep = mutableStateOf<IsoDep?>(null)
    var zkpIcaoData: ZkpIcaoData? = null

    fun getIsoDep() = _isoDep.value

    fun updateIsoDep(isoDep: IsoDep) {
        _isoDep.value = isoDep
    }

    fun clearIsoDep() {
        _isoDep.value = null
    }
}