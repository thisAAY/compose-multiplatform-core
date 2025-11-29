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

import net.sf.jni4net.attributes.ClrConstructor;
import net.sf.jni4net.attributes.ClrMethod;
import net.sf.jni4net.attributes.ClrType;
import net.sf.jni4net.inj.IClrProxy;
import net.sf.jni4net.inj.INJEnv;
import system.Enum;
import system.Object;
import system.Type;

@ClrType
public class WinPointerPlugin extends Object {
    private static Type staticType;

    protected WinPointerPlugin(INJEnv var1, long var2) {
        super(var1, var2);
    }

    @ClrConstructor("()V")
    public WinPointerPlugin() {
        super((INJEnv)null, 0L);
        __ctorWinPointerPlugin0(this);
    }

    @ClrMethod("()V")
    private static native void __ctorWinPointerPlugin0(IClrProxy var0);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/RealTimeStylusEnabledData;)V")
    public native void RealTimeStylusEnabled(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/RealTimeStylusDisabledData;)V")
    public native void RealTimeStylusDisabled(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusInRangeData;)V")
    public native void StylusInRange(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusOutOfRangeData;)V")
    public native void StylusOutOfRange(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusDownData;)V")
    public native void StylusDown(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusUpData;)V")
    public native void StylusUp(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusButtonDownData;)V")
    public native void StylusButtonDown(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/StylusButtonUpData;)V")
    public native void StylusButtonUp(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/InAirPacketsData;)V")
    public native void InAirPackets(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/PacketsData;)V")
    public native void Packets(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/SystemGestureData;)V")
    public native void SystemGesture(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/TabletAddedData;)V")
    public native void TabletAdded(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/TabletRemovedData;)V")
    public native void TabletRemoved(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/CustomStylusData;)V")
    public native void CustomStylusDataAdded(Object var1, Object var2);

    @ClrMethod("(LMicrosoft/StylusInput/RealTimeStylus;LMicrosoft/StylusInput/PluginData/ErrorData;)V")
    public native void Error(Object var1, Object var2);

    @ClrMethod("()LMicrosoft/StylusInput/DataInterestMask;")
    public native Enum getDataInterest();

    @ClrMethod("(LCsWinPointer/IWinPointerReader;LSystem/IntPtr;)V")
    public native void Initialize(IWinPointerReader var1, long var2);

    public static Type typeof() {
        return staticType;
    }

    private static void InitJNI(INJEnv var0, Type var1) {
        staticType = var1;
    }
}
