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

import net.sf.jni4net.attributes.ClrTypeInfo;
import net.sf.jni4net.inj.INJEnv;
import system.Type;

@ClrTypeInfo
public final class IWinPointerReader_ {
    private static Type staticType;

    public static Type typeof() {
        return staticType;
    }

    private static void InitJNI(INJEnv var0, Type var1) {
        staticType = var1;
    }
}
