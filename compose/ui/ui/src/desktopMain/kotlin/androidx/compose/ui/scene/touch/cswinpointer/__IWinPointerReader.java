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

package androidx.compose.ui.scene.touch.cswinpointer;

import net.sf.jni4net.attributes.ClrMethod;
import net.sf.jni4net.attributes.ClrProxy;
import net.sf.jni4net.inj.INJEnv;
import system.Object;

@ClrProxy
class __IWinPointerReader extends Object implements IWinPointerReader {
    protected __IWinPointerReader(INJEnv var1, long var2) {
        super(var1, var2);
    }

    @ClrMethod("(IIIZIII)V")
    public native void PointerXYEvent(int var1, int var2, int var3, boolean var4, int var5, int var6, int var7);

    @ClrMethod("(IIIZI)V")
    public native void PointerButtonEvent(int var1, int var2, int var3, boolean var4, int var5);

    @ClrMethod("(IIIZ)V")
    public native void PointerEvent(int var1, int var2, int var3, boolean var4);
}
