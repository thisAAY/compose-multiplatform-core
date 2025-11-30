/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scene.touch.cswinpointer

import com.sun.jna.CallbackReference
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.Kernel32
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * Bridge class that enables raw multi-touch input for Windows using JNA.
 * This class hooks directly into the Windows API to intercept WM_TOUCH messages,
 * bypassing the default mouse event synthesis.
 *
 * @param windowHandle The HWND (window handle) of the window to enable touch for
 * @param component The AWT Component to convert coordinates relative to
 * @param listener Callback interface to receive touch events
 */
internal class WindowsTouchBridge(
    private val windowHandle: Long,
    private val component: Component,
    private val listener: NativeTouchListener
) {
    private val user32 = TouchUser32.INSTANCE
    private val hWnd = WinDef.HWND(Pointer.createConstant(windowHandle))

    // Keep strong reference to prevent GC
    private var windowProcCallback: WindowProcCallback? = null

    // Store original window procedure (as Long for easier handling)
    private var originalWndProc: Long? = null

    private var isRegistered = false

    init {
        if (windowHandle == 0L) {
            throw IllegalArgumentException("Invalid window handle: 0")
        }
    }

    /**
     * Helper method to get detailed window information for debugging
     */
    private fun getWindowInfo(hWnd: WinDef.HWND): String {
        val classNameBuffer = com.sun.jna.Memory(256)
        val classNameLength = user32.GetClassNameW(hWnd, classNameBuffer, 256)
        val className = if (classNameLength > 0) {
            classNameBuffer.getWideString(0)
        } else {
            "Unknown"
        }
        
        val parentHwnd = user32.GetParent(hWnd)
        val rootHwnd = user32.GetAncestor(hWnd, 2) // GA_ROOT = 2
        
        val parentThread = if (parentHwnd != null && user32.IsWindow(parentHwnd)) {
            val pidMem = com.sun.jna.Memory(Int.SIZE_BYTES.toLong())
            val tid = user32.GetWindowThreadProcessId(parentHwnd, pidMem)
            pidMem.close()
            tid.toString()
        } else {
            "No parent"
        }
        
        val rootThread = if (rootHwnd != null && user32.IsWindow(rootHwnd)) {
            val pidMem = com.sun.jna.Memory(Int.SIZE_BYTES.toLong())
            val tid = user32.GetWindowThreadProcessId(rootHwnd, pidMem)
            pidMem.close()
            tid.toString()
        } else {
            "No root"
        }
        
        val parentHandleStr = parentHwnd?.pointer?.getLong(0)?.toString(16) ?: "None"
        val rootHandleStr = rootHwnd?.pointer?.getLong(0)?.toString(16) ?: "None"
        
        return "Class: '$className', Parent HWND: 0x$parentHandleStr, Root HWND: 0x$rootHandleStr, ParentThread: $parentThread, RootThread: $rootThread"
    }

    /**
     * Initialize the touch bridge by:
     * 1. Registering the window for touch input
     * 2. Subclassing the window procedure to intercept WM_TOUCH messages
     */
    fun initialize() {
        if (isRegistered) {
            return
        }

        // Validate window handle before attempting registration
        if (!user32.IsWindow(hWnd)) {
            throw IllegalStateException(
                "Invalid window handle (HWND). " +
                    "Window handle: 0x${windowHandle.toString(16)}, " +
                    "IsWindow check failed. " +
                    "The window may not be created yet or the handle is invalid."
            )
        }

        // Check if window belongs to this process and gather thread information for debugging
        val processId = Memory(Int.SIZE_BYTES.toLong())
        val windowThreadId = user32.GetWindowThreadProcessId(hWnd, processId)
        val windowProcessId = processId.getInt(0)
        val currentProcessId = Kernel32.INSTANCE.GetCurrentProcessId().toInt()
        val currentWindowsThreadId = Kernel32.INSTANCE.GetCurrentThreadId().toInt()
        val currentJavaThreadId = Thread.currentThread().id
        val isOnEDT = SwingUtilities.isEventDispatchThread()

        // Get detailed window information
        val windowInfo = getWindowInfo(hWnd)
        
        // Check parent and root window threads
        val parentHwnd = user32.GetParent(hWnd)
        val rootHwnd = user32.GetAncestor(hWnd, 2) // GA_ROOT = 2
        
        var parentWindowThreadId: String = "N/A"
        var rootWindowThreadId: String = "N/A"
        
        if (parentHwnd != null && user32.IsWindow(parentHwnd)) {
            val parentPidMem = Memory(Int.SIZE_BYTES.toLong())
            val parentThreadId = user32.GetWindowThreadProcessId(parentHwnd, parentPidMem)
            parentPidMem.close()
            parentWindowThreadId = parentThreadId.toString()
        }
        
        if (rootHwnd != null && user32.IsWindow(rootHwnd)) {
            val rootPidMem = Memory(Int.SIZE_BYTES.toLong())
            val rootThreadId = user32.GetWindowThreadProcessId(rootHwnd, rootPidMem)
            rootPidMem.close()
            rootWindowThreadId = rootThreadId.toString()
        }

        // Debug output - log all thread information
        println("=== WindowsTouchBridge Thread Debug Info ===")
        println("Window handle: 0x${windowHandle.toString(16)}")
        println("Window info: $windowInfo")
        println("Window thread ID (Windows): $windowThreadId")
        println("Window process ID: $windowProcessId")
        println("Current process ID: $currentProcessId")
        println("Current Windows thread ID: $currentWindowsThreadId")
        println("Current Java thread ID: $currentJavaThreadId")
        println("Current thread name: ${Thread.currentThread().name}")
        println("Is on EDT: $isOnEDT")
        println("Threads match: ${windowThreadId == currentWindowsThreadId}")
        if (parentHwnd != null) {
            println("Parent window handle: 0x${parentHwnd.pointer.getLong(0).toString(16)}")
            println("Parent window thread ID: $parentWindowThreadId")
        }
        if (rootHwnd != null) {
            println("Root window handle: 0x${rootHwnd.pointer.getLong(0).toString(16)}")
            println("Root window thread ID: $rootWindowThreadId")
        }
        println("============================================")

        if (windowProcessId != currentProcessId) {
            processId.close()
            throw IllegalStateException(
                "Window does not belong to current process. " +
                    "Window process ID: $windowProcessId, " +
                    "Current process ID: $currentProcessId. " +
                    "RegisterTouchWindow can only be called on windows owned by the calling process."
            )
        }
        processId.close()

        // Register window for touch input
        val registered = user32.RegisterTouchWindow(hWnd, WinDef.UINT(0))
        if (!registered) {
            val error = Kernel32.INSTANCE.GetLastError()
            val errorCode = error.toInt()
            val errorDescription = when (errorCode) {
                5 -> "ERROR_ACCESS_DENIED - The calling thread does not own the specified window. " +
                    "Window thread ID: $windowThreadId, Current thread ID: $currentWindowsThreadId"
                87 -> "ERROR_INVALID_PARAMETER - The hWnd parameter is invalid"
                else -> "Unknown error code"
            }
            throw IllegalStateException(
                "Failed to register window for touch input. " +
                    "Windows error code: $errorCode ($errorDescription). " +
                    "Window handle: 0x${windowHandle.toString(16)}. " +
                    "Thread ownership: Window belongs to thread $windowThreadId, " +
                    "but RegisterTouchWindow was called from thread $currentWindowsThreadId."
            )
        }

        // Create and install window procedure callback
        windowProcCallback = WindowProcCallback()
        
        // Subclass the window to intercept messages
        originalWndProc = user32.GetWindowLongPtrForSubclass(hWnd, WindowsTouchConstants.GWLP_WNDPROC)
        
        // Get the callback pointer from the callback object
        val callbackFunctionPointer = CallbackReference.getFunctionPointer(windowProcCallback!!)

        val result = user32.SetWindowLongPtrForSubclass(hWnd, WindowsTouchConstants.GWLP_WNDPROC,
            callbackFunctionPointer
        )
        val lastError = Kernel32.INSTANCE.GetLastError()
        if (result == 0L && lastError.toInt() != 0) {
            throw IllegalStateException("Failed to subclass window procedure. Error: $lastError")
        }

        isRegistered = true
    }

    /**
     * Clean up resources and restore the original window procedure
     */
    fun dispose() {
        if (!isRegistered) {
            return
        }

        try {
            // Restore original window procedure
            originalWndProc?.let { original ->
                val originalPtr = Pointer.createConstant(original)
                user32.SetWindowLongPtrForSubclass(hWnd, WindowsTouchConstants.GWLP_WNDPROC, originalPtr)
            }

            // Unregister touch window (if needed)
            // Note: There's no explicit unregister function, but we can just ignore further messages

        } catch (e: Exception) {
            // Log but don't throw - cleanup should be best-effort
            System.err.println("Error during WindowsTouchBridge disposal: ${e.message}")
        } finally {
            windowProcCallback = null
            originalWndProc = null
            isRegistered = false
        }
    }

    /**
     * Window procedure callback that intercepts WM_TOUCH messages.
     * This is kept as a class field to prevent garbage collection.
     */
    private inner class WindowProcCallback : WinUser.WindowProc {
        override fun callback(
            hWnd: WinDef.HWND,
            uMsg: Int,
            wParam: WinDef.WPARAM,
            lParam: WinDef.LPARAM
        ): WinDef.LRESULT {
            if (uMsg == WindowsTouchConstants.WM_TOUCH) {
                handleTouchMessage(wParam, lParam)
                // Return 0 to indicate we handled the message and prevent mouse event synthesis
                return WinDef.LRESULT(0)
            }

            // For all other messages, call the original window procedure
            return if (originalWndProc != null) {
                user32.CallWindowProc(originalWndProc!!, hWnd, uMsg, wParam, lParam)
            } else {
                WinDef.LRESULT(0)
            }
        }
    }

    /**
     * Handles WM_TOUCH message by parsing TOUCHINPUT structures and converting coordinates
     */
    private fun handleTouchMessage(wParam: WinDef.WPARAM, lParam: WinDef.LPARAM) {
        val touchInputHandle = WinNT.HANDLE(lParam.toPointer())
        // WPARAM contains the count as low-order word
        val wParamValue = Pointer.nativeValue(wParam.toPointer())
        val inputCount = (wParamValue and 0xFFFF).toLong()

        if (inputCount <= 0) {
            return
        }

        // Allocate memory for TOUCHINPUT structures
        val touchInputSize = TOUCHINPUT().size()
        val touchInputsMemory = Memory(touchInputSize * inputCount)

        // Get touch input data
        val success = user32.GetTouchInputInfo(
            touchInputHandle,
            WinDef.UINT(inputCount),
            touchInputsMemory,
            touchInputSize
        )

        if (!success) {
            return
        }

        try {
            // Parse each touch input
            for (i in 0 until inputCount) {
                val touchInputPtr = touchInputsMemory.share((touchInputSize * i).toLong(), touchInputSize.toLong())
                val touchInput = TOUCHINPUT(touchInputPtr)

                val state = when {
                    touchInput.isDown() -> TouchState.DOWN
                    touchInput.isUp() -> TouchState.UP
                    touchInput.isMove() -> TouchState.MOVE
                    else -> continue // Unknown state, skip
                }

                // Convert coordinates from screen (1/100th pixels) to component-relative pixels
                val screenX = touchInput.x / 100.0
                val screenY = touchInput.y / 100.0

                // Convert screen coordinates to component-relative coordinates
                val componentLocation = component.locationOnScreen
                val relativeX = (screenX - componentLocation.x).toInt()
                val relativeY = (screenY - componentLocation.y).toInt()

                // Notify listener on EDT to ensure thread safety
                SwingUtilities.invokeLater {
                    listener.onTouchEvent(
                        fingerId = touchInput.dwID,
                        x = relativeX,
                        y = relativeY,
                        state = state
                    )
                }
            }
        } finally {
            // Close the touch input handle
            user32.CloseTouchInputHandle(touchInputHandle)
        }
    }
}
