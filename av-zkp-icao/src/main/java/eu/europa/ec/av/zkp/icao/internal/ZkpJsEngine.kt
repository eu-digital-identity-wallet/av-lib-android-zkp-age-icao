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
package eu.europa.ec.av.zkp.icao.internal

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper around a headless WebView to run the ZKP JS code.
 *
 *   generateArtifactsFromAndroid(inputPassportJson: string)
 *
 * JS side is responsible for calling:
 *   Android.onArtifactsReady(json: string)
 *   Android.onZkpError(message: string)
 */

internal class ZkpJsEngine(
    private val context: Context
) {

    private val webViewDeferred = CompletableDeferred<WebView>()

    @Volatile
    private var pendingCall: PendingCall? = null

    private data class PendingCall(
        val continuation: Continuation<String>
    )

    /**
     * This object is exposed to JS as `window.Android`.
     * Must match what you declared in TypeScript:
     *
     * declare const Android: {
     *   onArtifactsReady?(json: string): void;
     *   onZkpError?(message: string): void;
     * } | undefined;
     */
    private inner class AndroidBridge {

        @JavascriptInterface
        fun onArtifactsReady(json: String) {
            val call = pendingCall ?: return
            pendingCall = null
            call.continuation.resume(json)
        }

        @JavascriptInterface
        fun onZkpError(message: String) {
            val call = pendingCall ?: return
            pendingCall = null
            call.continuation.resumeWithException(
                IllegalStateException("ZKP error from JS: $message")
            )
        }
    }

    /**
     * Initialize the headless WebView and load the HTML from assets.
     * Call this once before using [generateArtifacts].
     */
    suspend fun init() {
        withContext(Dispatchers.Main) {
            if (!webViewDeferred.isCompleted) {
                val webView = WebView(context).apply {
                    @Suppress("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!webViewDeferred.isCompleted) {
                                webViewDeferred.complete(this@apply)
                            }
                        }
                    }

                    addJavascriptInterface(AndroidBridge(), "Android")
                    loadUrl("file:///android_asset/zkp-icao.html")
                }
            }
        }

        webViewDeferred.await()
    }

    /**
     * Calls the JS function:
     *
     *   generateArtifactsFromAndroid(inputPassportJson: string)
     *
     * and waits until JS calls Android.onArtifactsReady(json) or Android.onZkpError(msg).
     *
     * @param inputPassportJson Input passport data in JSON format.
     * @return JSON string: { "dsc": {...}, "id": {...} }
     */
    suspend fun generateArtifacts(inputPassportJson: String): String =
        withContext(Dispatchers.Main) {
            if (pendingCall != null) {
                throw IllegalStateException("ZkpJsEngine: another call is already in progress")
            }

            val webView = webViewDeferred.await()

            val arg = JSONObject.quote(inputPassportJson)
            val js = "generateArtifactsFromAndroid($arg)"

            suspendCancellableCoroutine { cont ->
                pendingCall = PendingCall(cont)
                webView.evaluateJavascript(js, null)
            }
        }
}