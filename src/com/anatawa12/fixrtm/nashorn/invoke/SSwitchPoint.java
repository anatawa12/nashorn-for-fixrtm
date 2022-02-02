/*
 * this file is created and modified only by anatawa12 (not openjdk team).
 * if contribute this file, you have to permit for anatawa12 to change this file's license.
 *
 * The files in package may be going to change license and split to a single module.
 */
/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.anatawa12.fixrtm.nashorn.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.SwitchPoint;

/**
 * @see SwitchPoint
 */
public class SSwitchPoint {
    private static final SMethodHandle trueHandle = SMethodHandles.constant(boolean.class, true);
    private static final SMethodHandle falseHandle = SMethodHandles.constant(boolean.class, false);

    private final SMutableCallSite callSite;
    private final SMethodHandle invoker;

    /**
     * Creates a new switch point.
     */
    public SSwitchPoint() {
        this.callSite = new SMutableCallSite(trueHandle);
        this.invoker = callSite.sdynamicInvoker();
    }

    /**
     * @see SwitchPoint#hasBeenInvalidated() 
     * @return same as {@link SwitchPoint#guardWithTest(MethodHandle, MethodHandle)}
     */
    public boolean hasBeenInvalidated() {
        return (callSite.getSTarget() != trueHandle);
    }

    /**
     * @see SwitchPoint#guardWithTest(MethodHandle, MethodHandle) 
     * @param target same as {@link SwitchPoint#guardWithTest(MethodHandle, MethodHandle)}
     * @param fallback same as {@link SwitchPoint#guardWithTest(MethodHandle, MethodHandle)}
     * @return same as {@link SwitchPoint#guardWithTest(MethodHandle, MethodHandle)}
     */
    public SMethodHandle guardWithTest(SMethodHandle target, SMethodHandle fallback) {
        if (callSite.getSTarget() == falseHandle)
            return fallback;  // already invalid
        return SMethodHandles.guardWithTest(invoker, target, fallback);
    }

    /**
     * @see SwitchPoint#invalidateAll(SwitchPoint[])
     * @param switchPoints same as {@link SwitchPoint#invalidateAll(SwitchPoint[])}
     */
    public static void invalidateAll(SSwitchPoint[] switchPoints) {
        // if not Points, nop.
        if (switchPoints.length == 0)  return;
        // this implantation is by javadoc.
        SMutableCallSite[] sites = new SMutableCallSite[switchPoints.length];
        for (int i = 0; i < switchPoints.length; i++) {
            SSwitchPoint switchPoint = switchPoints[i];
            if (switchPoint == null) break;
            sites[i] = switchPoint.callSite;
            switchPoint.callSite.setTarget(falseHandle);
        }
        SMutableCallSite.syncAll(sites);
    }
}
