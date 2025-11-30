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

/**
 * Represents the state of a touch event
 */
internal enum class TouchState {
    DOWN,
    MOVE,
    UP
}

/**
 * Listener interface for receiving native Windows touch events.
 * Coordinates provided are in component-relative pixels.
 */
internal interface NativeTouchListener {
    /**
     * Called when a touch event occurs
     *
     * @param fingerId Unique identifier for this touch point (Windows touch ID)
     * @param x X coordinate relative to the component (in pixels)
     * @param y Y coordinate relative to the component (in pixels)
     * @param state The state of the touch (DOWN, MOVE, or UP)
     */
    fun onTouchEvent(fingerId: Int, x: Int, y: Int, state: TouchState)
}

