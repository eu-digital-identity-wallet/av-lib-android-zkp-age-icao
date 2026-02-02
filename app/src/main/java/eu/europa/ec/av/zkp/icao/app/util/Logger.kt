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

import android.util.Log
import eu.europa.ec.av.zkp.icao.ZkpLogger

/**
 * Utility class for logging that handles long messages by splitting them into smaller chunks.
 */
internal object Log {

    private const val CHUNK_SIZE = 3900

    private fun isLong(msg: String) = msg.length > CHUNK_SIZE

    fun d(tag: String, msg: String) {
        if (isLong(msg)) logLong(tag, msg, Log.DEBUG) else Log.d(tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (isLong(msg)) {
            logLong(tag, msg, Log.ERROR)
            if (tr != null) Log.e(tag, "stacktrace:", tr)
        } else {
            if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (isLong(msg)) logLong(tag, msg, Log.INFO) else Log.i(tag, msg)
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (isLong(msg)) {
            logLong(tag, msg, Log.WARN)
            if (tr != null) Log.w(tag, "stacktrace:", tr)
        } else {
            if (tr != null) Log.w(tag, msg, tr) else Log.w(tag, msg)
        }
    }

    fun v(tag: String, msg: String) {
        if (isLong(msg)) logLong(tag, msg, Log.VERBOSE) else Log.v(tag, msg)
    }

    private fun logLong(tag: String, msg: String, level: Int = Log.DEBUG) {
        var i = 0
        while (i < msg.length) {
            val end = (i + CHUNK_SIZE).coerceAtMost(msg.length)
            val part = msg.substring(i, end)
            when (level) {
                Log.ERROR -> Log.e(tag, part)
                Log.WARN -> Log.w(tag, part)
                Log.INFO -> Log.i(tag, part)
                Log.VERBOSE -> Log.v(tag, part)
                else -> Log.d(tag, part)
            }
            i = end
        }
    }
}

/**
 * Implementation of [ZkpLogger] that uses the app's logging utility.
 */
internal val zkpLogger = object : ZkpLogger {
    override fun d(tag: String, msg: String) = eu.europa.ec.av.zkp.icao.app.util.Log.d(tag, msg)
    override fun e(tag: String, msg: String, tr: Throwable?) =
        eu.europa.ec.av.zkp.icao.app.util.Log.e(tag, msg, tr)

    override fun i(tag: String, msg: String) = eu.europa.ec.av.zkp.icao.app.util.Log.i(tag, msg)
    override fun w(tag: String, msg: String, tr: Throwable?) =
        eu.europa.ec.av.zkp.icao.app.util.Log.w(tag, msg, tr)

    override fun v(tag: String, msg: String) = eu.europa.ec.av.zkp.icao.app.util.Log.v(tag, msg)
}