/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/**
 * Test to check that nashorn "internal" classes in codegen, parser, ir
 * packages cannot * be accessed from sandbox scripts.
 *
 * @test
 * @run
 * @security
 */

function checkClass(name) {
    try {
        Java.type(name);
        fail("should have thrown exception for: " + name);
    } catch (e) {
        if (! (e instanceof java.lang.SecurityException)) {
            fail("Expected SecurityException, but got " + e);
        }
    }
}

// Not exhaustive - but a representative list of classes
checkClass("com.anatawa12.fixrtm.nashorn.internal.codegen.Compiler");
checkClass("com.anatawa12.fixrtm.nashorn.internal.codegen.types.Type");
checkClass("com.anatawa12.fixrtm.nashorn.internal.ir.Node");
checkClass("com.anatawa12.fixrtm.nashorn.internal.ir.FunctionNode");
checkClass("com.anatawa12.fixrtm.nashorn.internal.ir.debug.JSONWriter");
checkClass("com.anatawa12.fixrtm.nashorn.internal.ir.visitor.NodeVisitor");
checkClass("com.anatawa12.fixrtm.nashorn.internal.lookup.MethodHandleFactory");
checkClass("com.anatawa12.fixrtm.nashorn.internal.objects.Global");
checkClass("com.anatawa12.fixrtm.nashorn.internal.parser.AbstractParser");
checkClass("com.anatawa12.fixrtm.nashorn.internal.parser.Parser");
checkClass("com.anatawa12.fixrtm.nashorn.internal.parser.JSONParser");
checkClass("com.anatawa12.fixrtm.nashorn.internal.parser.Lexer");
checkClass("com.anatawa12.fixrtm.nashorn.internal.parser.Scanner");
checkClass("com.anatawa12.fixrtm.nashorn.internal.runtime.Context");
checkClass("com.anatawa12.fixrtm.nashorn.internal.runtime.arrays.ArrayData");
checkClass("com.anatawa12.fixrtm.nashorn.internal.runtime.linker.Bootstrap");
checkClass("com.anatawa12.fixrtm.nashorn.internal.runtime.options.Option");
checkClass("com.anatawa12.fixrtm.nashorn.internal.runtime.regexp.RegExp");
checkClass("com.anatawa12.fixrtm.nashorn.internal.scripts.JO");
checkClass("com.anatawa12.fixrtm.nashorn.tools.Shell");
checkClass("com.anatawa12.fixrtm.nashorn.dynalink.CallSiteDescriptor");
checkClass("com.anatawa12.fixrtm.nashorn.dynalink.beans.StaticClass");
checkClass("com.anatawa12.fixrtm.nashorn.dynalink.linker.LinkRequest");
checkClass("com.anatawa12.fixrtm.nashorn.dynalink.support.AbstractRelinkableCallSite");
