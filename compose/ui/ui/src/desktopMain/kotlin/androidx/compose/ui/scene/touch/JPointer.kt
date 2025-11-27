/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.scene.touch

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import cswinpointer.IWinPointerReader
import cswinpointer.WinPointerPlugin
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFrame
import net.sf.jni4net.Bridge

class JWinPointerReader2(windowName: String?) : IWinPointerReader {
    private val APP_DIR = "JWinPointer\\"
    private val _pointerEventListeners: MutableList<PointerEventListener> = mutableListOf()

    fun addPointerEventListener(listener: PointerEventListener?) {
        this._pointerEventListeners.add(listener!!)
    }

    constructor(window: JFrame) : this(window.getTitle())

    init {
        this.initializeBridge()
        val hWnd = User32.INSTANCE.FindWindow(null as String?, windowName)
        val nativeHandle = Pointer.nativeValue(hWnd.getPointer())
        val plugin = WinPointerPlugin()
        plugin.Initialize(this, nativeHandle)
    }

    private fun initializeBridge() {
        try {
            val model = System.getProperty("sun.arch.data.model").toInt()
            val jni4netLib = "jni4net.n.w" + model + ".v40-0.8.8.0.dll"
            val appData = System.getProperty("java.io.tmpdir") + "JWinPointer\\"
            this.extractDependencies(appData)
            Bridge.setVerbose(true)
            val jni4netPath = appData + jni4netLib
            Bridge.init(File(jni4netPath))
            Bridge.LoadAndRegisterAssemblyFrom(File(appData + "CsWinPointer.j4n.dll"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

     fun extractDependencies(appDataDir: String) {
        val directory = File(appDataDir)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        try {
            // List of known resource files to extract
            val resourceFiles = listOf(
                "CsWinPointer.dll",
                "CsWinPointer.j4n.dll",
                "jni4net.j-0.8.8.0.jar",
                "jni4net.n-0.8.8.0.dll",
                "jni4net.n.w32.v40-0.8.8.0.dll",
                "jni4net.n.w64.v40-0.8.8.0.dll",
                "Microsoft.Ink.dll"
            )

            // Use the context class loader to access resources
            val loader = Thread.currentThread().contextClassLoader
                ?: JWinPointerReader2::class.java.classLoader

            for (fileName in resourceFiles) {
                // Get resource as stream from classpath
                val resourceStream = loader.getResourceAsStream(fileName)

                if (resourceStream != null) {
                    val fileOut = File(appDataDir + fileName)
                    Files.copy(
                        resourceStream,
                        fileOut.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    resourceStream.close()
                    println("Extracted: ${fileOut.absolutePath}")
                } else {
                    println("Warning: Resource not found: $fileName")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun PointerXYEvent(
        deviceType: Int,
        pointerID: Int,
        eventType: Int,
        inverted: Boolean,
        x: Int,
        y: Int,
        pressure: Int
    ) {
        for (listener in this._pointerEventListeners) {
            listener.pointerXYEvent(deviceType, pointerID, eventType, inverted, x, y, pressure)
        }
    }

    override fun PointerButtonEvent(
        deviceType: Int,
        pointerID: Int,
        eventType: Int,
        inverted: Boolean,
        buttonIndex: Int
    ) {
        for (listener in this._pointerEventListeners) {
            listener.pointerButtonEvent(deviceType, pointerID, eventType, inverted, buttonIndex)
        }
    }

    override fun PointerEvent(deviceType: Int, pointerID: Int, eventType: Int, inverted: Boolean) {
        for (listener in this._pointerEventListeners) {
            listener.pointerEvent(deviceType, pointerID, eventType, inverted)
        }
    }

    interface PointerEventListener {
        fun pointerXYEvent(
            var1: Int,
            var2: Int,
            var3: Int,
            var4: Boolean,
            var5: Int,
            var6: Int,
            var7: Int
        )

        fun pointerButtonEvent(var1: Int, var2: Int, var3: Int, var4: Boolean, var5: Int)

        fun pointerEvent(var1: Int, var2: Int, var3: Int, var4: Boolean)
    }
}