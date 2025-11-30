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
import com.sun.jna.platform.win32.BaseTSD
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
        println("Window thread ID (Windows): $windowThreadId")
        println("Window process ID: $windowProcessId")
        println("Current process ID: $currentProcessId")
        println("Current Windows thread ID: $currentWindowsThreadId")
        println("Current Java thread ID: $currentJavaThreadId")
        println("Current thread name: ${Thread.currentThread().name}")
        println("Is on EDT: $isOnEDT")
        println("Threads match: ${windowThreadId == currentWindowsThreadId}")

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

        // Install WindowProc FIRST so we can handle the registration message on the window's thread
        windowProcCallback = WindowProcCallback()
        
        // Subclass the window to intercept messages
        val originalPtr = user32.GetWindowLongPtrA(hWnd, WindowsTouchConstants.GWLP_WNDPROC)
        originalWndProc = Pointer.nativeValue(originalPtr.toPointer())
        
        // Get the callback pointer from the callback object
        val callbackFunctionPointer = CallbackReference.getFunctionPointer(windowProcCallback!!)
        val callbackNativeValue = Pointer.nativeValue(callbackFunctionPointer)
        val callbackAsLongPtr = BaseTSD.LONG_PTR(callbackNativeValue)

        val result = user32.SetWindowLongPtrA(hWnd, WindowsTouchConstants.GWLP_WNDPROC, callbackAsLongPtr)
        val lastError = Kernel32.INSTANCE.GetLastError()
        val resultValue = Pointer.nativeValue(result.toPointer())
        if (resultValue == 0L && lastError.toInt() != 0) {
            throw IllegalStateException("Failed to subclass window procedure. Error: $lastError")
        }

        // Now dispatch the registration to the window's thread using SendMessage
        // HOW IT WORKS:
        // 1. SendMessage sends WM_REGISTER_TOUCH message to the window
        // 2. Windows routes the message to our WindowProc callback (which we just installed)
        // 3. WindowProc runs on the window's owner thread (Windows guarantees this)
        // 4. Our WindowProc handles WM_REGISTER_TOUCH and calls RegisterTouchWindow there
        // This ensures RegisterTouchWindow is called on the correct thread
        if (windowThreadId != currentWindowsThreadId) {
            println("Threads don't match - dispatching RegisterTouchWindow to window's thread via SendMessage...")
            println("Sending WM_REGISTER_TOUCH message - it will be handled by WindowProc on thread $windowThreadId")
            val registrationResult = user32.SendMessageA(
                hWnd,
                WindowsTouchConstants.WM_REGISTER_TOUCH,
                WinDef.WPARAM(0),
                WinDef.LPARAM(0)
            )
            
            if (registrationResult.toInt() == 0) {
                // Registration failed - check error
                val error = Kernel32.INSTANCE.GetLastError()
                val errorCode = error.toInt()
                throw IllegalStateException(
                    "Failed to register touch window via SendMessage. " +
                        "Windows error code: $errorCode. " +
                        "Window handle: 0x${windowHandle.toString(16)}. " +
                        "The registration was attempted on the window's thread ($windowThreadId) but failed."
                )
            }
            println("Successfully registered touch window on window's thread!")
        } else {
            // Threads match - can register directly
            val registered = user32.RegisterTouchWindow(hWnd, WinDef.UINT(0))
            if (!registered) {
                val error = Kernel32.INSTANCE.GetLastError()
                val errorCode = error.toInt()
                throw IllegalStateException(
                    "Failed to register window for touch input. " +
                        "Windows error code: $errorCode. " +
                        "Window handle: 0x${windowHandle.toString(16)}."
                )
            }
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
                val originalAsLongPtr = BaseTSD.LONG_PTR(original)
                user32.SetWindowLongPtrA(hWnd, WindowsTouchConstants.GWLP_WNDPROC, originalAsLongPtr)
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
            when (uMsg) {
                WindowsTouchConstants.WM_REGISTER_TOUCH -> {
                    // This WindowProc callback runs on the window's owner thread (Windows guarantees this)
                    // SendMessage from initialize() sent us this custom message, so we can now safely
                    // call RegisterTouchWindow on the correct thread
                    println("WM_REGISTER_TOUCH received on thread ${Kernel32.INSTANCE.GetCurrentThreadId()}")
                    println("Calling RegisterTouchWindow now - we're on the window's thread!")
                    val registered = user32.RegisterTouchWindow(hWnd, WinDef.UINT(0))
                    if (!registered) {
                        val error = Kernel32.INSTANCE.GetLastError()
                        val errorCode = error.toInt()
                        println("Failed to register touch window on window's thread. Error code: $errorCode")
                        return WinDef.LRESULT(0) // Return 0 to indicate failure
                    }
                    println("Successfully registered touch window in WindowProc callback!")
                    return WinDef.LRESULT(1) // Return 1 to indicate success
                }
                WindowsTouchConstants.WM_TOUCH -> {
                    handleTouchMessage(wParam, lParam)
                    // Return 0 to indicate we handled the message and prevent mouse event synthesis
                    return WinDef.LRESULT(0)
                }
            }

            // For all other messages, call the original window procedure
            return if (originalWndProc != null) {
                user32.CallWindowProcA(originalWndProc!!, hWnd, uMsg, wParam, lParam)
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
