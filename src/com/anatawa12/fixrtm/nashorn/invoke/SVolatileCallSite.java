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
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;

public class SVolatileCallSite extends VolatileCallSite implements SCallSite {
    public volatile SMethodHandle real;

    public SVolatileCallSite(MethodType type) {
        this(Utils.makeUninitializedCallSite(type));
    }

    public SVolatileCallSite(SMethodHandle target) {
        super(target.getReal());
        real = target;
    }

    public void setTarget(SMethodHandle newTarget) {
        super.setTarget(newTarget.getReal());
        real = newTarget;
    }

    public void setTarget(MethodHandle newTarget) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SMethodHandle sdynamicInvoker() {
        return Utils.makeDynamicInvoker(this);
    }

    @Override
    public SMethodHandle getSTarget() {
        return real;
    }
}
