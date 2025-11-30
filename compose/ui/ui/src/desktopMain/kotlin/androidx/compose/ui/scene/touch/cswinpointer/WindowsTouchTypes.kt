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

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser

/**
 * Windows message constants
 */
internal object WindowsTouchConstants {
    const val WM_TOUCH = 0x0240
    // Custom message to register touch on window's thread
    // WM_USER = 0x0400, so 0x0401 is WM_USER + 1 (first available custom message)
    // This is used internally to dispatch RegisterTouchWindow to the correct thread
    const val WM_REGISTER_TOUCH = 0x0401
    const val GWLP_WNDPROC = -4
    const val TOUCHEVENTF_MOVE = 0x0001
    const val TOUCHEVENTF_DOWN = 0x0002
    const val TOUCHEVENTF_UP = 0x0004
    const val TOUCHINPUTMASKF_TIMEFROMSYSTEM = 0x0001
}

/**
 * Extended User32 interface that includes touch-related Windows APIs
 */
internal interface TouchUser32 : User32 {
    /**
     * Retrieves the identifier of the thread that created the specified window.
     *
     * @param hWnd A handle to the window.
     * @return The return value is the identifier of the thread that created the window.
     */
    fun GetWindowThreadProcessId(hWnd: WinDef.HWND, lpdwProcessId: Pointer?): Int

    /**
     * Retrieves the name of the class to which the specified window belongs.
     *
     * @param hWnd A handle to the window.
     * @param lpClassName A pointer to the buffer that is to receive the class name string.
     * @param nMaxCount The length of the buffer.
     * @return The number of characters copied to the buffer, not including the terminating null character.
     */
    fun GetClassNameW(hWnd: WinDef.HWND, lpClassName: Pointer, nMaxCount: Int): Int

    /**
     * Registers a window as being touch-capable.
     *
     * @param hWnd The handle to the window to register.
     * @param ulFlags Flags that specify optional modifications.
     * @return Non-zero if the function succeeds, zero otherwise.
     */
    fun RegisterTouchWindow(hWnd: WinDef.HWND, ulFlags: WinDef.UINT): Boolean

    /**
     * Retrieves detailed information about touch inputs associated with a particular touch input handle.
     *
     * @param hTouchInput The touch input handle received in the WM_TOUCH message.
     * @param cInputs The number of structures in the pInputs array.
     * @param pInputs A pointer to an array of TOUCHINPUT structures to receive information about the touch points.
     * @param cbSize The size, in bytes, of a single TOUCHINPUT structure.
     * @return Non-zero if the function succeeds, zero otherwise.
     */
    fun GetTouchInputInfo(
        hTouchInput: WinNT.HANDLE,
        cInputs: WinDef.UINT,
        pInputs: Pointer,
        cbSize: Int
    ): Boolean

    /**
     * Closes a touch input handle, freeing the memory associated with it.
     *
     * @param hTouchInput The touch input handle received in the WM_TOUCH message.
     * @return Non-zero if the function succeeds, zero otherwise.
     */
    fun CloseTouchInputHandle(hTouchInput: WinNT.HANDLE): Boolean

    /**
     * Retrieves information about the specified window (pointer-sized version for 64-bit compatibility).
     * Used here for getting the original window procedure.
     * Using the ANSI version (GetWindowLongPtrA) explicitly.
     *
     * @param hWnd A handle to the window.
     * @param nIndex The zero-based offset to the value to be retrieved (GWLP_WNDPROC = -4).
     * @return The requested value as a LONG_PTR if the function succeeds, zero otherwise.
     */
    fun GetWindowLongPtrA(hWnd: WinDef.HWND, nIndex: Int): BaseTSD.LONG_PTR

    /**
     * Changes an attribute of the specified window (pointer-sized version for 64-bit compatibility).
     * Used here for setting the window procedure (subclassing).
     * Using the ANSI version (SetWindowLongPtrA) explicitly.
     *
     * @param hWnd A handle to the window.
     * @param nIndex The zero-based offset to the value to be set (GWLP_WNDPROC = -4).
     * @param dwNewLong The replacement value as a LONG_PTR.
     * @return The previous value as a LONG_PTR if the function succeeds, zero otherwise.
     */
    fun SetWindowLongPtrA(hWnd: WinDef.HWND, nIndex: Int, dwNewLong: BaseTSD.LONG_PTR): BaseTSD.LONG_PTR

    /**
     * Passes message information to the specified window procedure.
     * Used to call the original window procedure for messages we don't handle.
     *
     * @param lpPrevWndFunc The previous window procedure as a Long value.
     * @param hWnd A handle to the window.
     * @param uMsg The message.
     * @param wParam Additional message-specific information.
     * @param lParam Additional message-specific information.
     * @return The return value specifies the result of the message processing.
     */
    fun CallWindowProc(
        lpPrevWndFunc: Long,
        hWnd: WinDef.HWND,
        uMsg: Int,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): WinDef.LRESULT

    companion object {
        val INSTANCE: TouchUser32 = Native.load("user32", TouchUser32::class.java) as TouchUser32
    }
}

/**
 * Represents the TOUCHINPUT structure from Windows API.
 * This structure contains information about a single touch input point.
 *
 * Note: x and y are in 1/100th of a pixel (HIMETRIC units)
 */
@Structure.FieldOrder(
    "x",
    "y",
    "hSource",
    "dwID",
    "dwFlags",
    "dwMask",
    "dwTime",
    "dwExtraInfo",
    "cxContact",
    "cyContact"
)
internal class TOUCHINPUT : Structure {
    @JvmField
    var x: Int = 0

    @JvmField
    var y: Int = 0

    @JvmField
    var hSource: WinNT.HANDLE? = null

    @JvmField
    var dwID: Int = 0

    @JvmField
    var dwFlags: Int = 0

    @JvmField
    var dwMask: Int = 0

    @JvmField
    var dwTime: Int = 0

    @JvmField
    var dwExtraInfo: Pointer? = null

    @JvmField
    var cxContact: Int = 0

    @JvmField
    var cyContact: Int = 0

    constructor() : super()

    constructor(pointer: Pointer) : super(pointer) {
        read()
    }

    /**
     * Checks if this touch input represents a touch down event
     */
    fun isDown(): Boolean = (dwFlags and WindowsTouchConstants.TOUCHEVENTF_DOWN) != 0

    /**
     * Checks if this touch input represents a touch move event
     */
    fun isMove(): Boolean = (dwFlags and WindowsTouchConstants.TOUCHEVENTF_MOVE) != 0

    /**
     * Checks if this touch input represents a touch up event
     */
    fun isUp(): Boolean = (dwFlags and WindowsTouchConstants.TOUCHEVENTF_UP) != 0

    override fun read() {
        super.read()
    }

    override fun write() {
        super.write()
    }
}
