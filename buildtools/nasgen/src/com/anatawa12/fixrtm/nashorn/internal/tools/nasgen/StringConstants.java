/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.anatawa12.fixrtm.nashorn.internal.tools.nasgen;

import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;

import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Type;
import com.anatawa12.fixrtm.nashorn.internal.runtime.AccessorProperty;
import com.anatawa12.fixrtm.nashorn.internal.runtime.PropertyMap;
import com.anatawa12.fixrtm.nashorn.internal.runtime.PrototypeObject;
import com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptFunction;
import com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptObject;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Specialization;

import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;

/**
 * String constants used for code generation/instrumentation.
 */
@SuppressWarnings("javadoc")
public interface StringConstants {
    // standard jdk types, methods
    static final Type TYPE_METHODHANDLE         = Type.getType(SMethodHandle.class);
    static final Type TYPE_METHODHANDLE_ARRAY   = Type.getType(SMethodHandle[].class);
    static final Type TYPE_SPECIALIZATION       = Type.getType(Specialization.class);
    static final Type TYPE_SPECIALIZATION_ARRAY = Type.getType(Specialization[].class);
    static final Type TYPE_OBJECT               = Type.getType(Object.class);
    static final Type TYPE_STRING               = Type.getType(String.class);
    static final Type TYPE_CLASS                = Type.getType(Class.class);
    static final Type TYPE_COLLECTION           = Type.getType(Collection.class);
    static final Type TYPE_COLLECTIONS          = Type.getType(Collections.class);
    static final Type TYPE_ARRAYLIST            = Type.getType(ArrayList.class);
    static final Type TYPE_LIST                 = Type.getType(List.class);
    static final Type TYPE_CALL_SITE            = Type.getType(CallSite.class);
    static final Type TYPE_METHOD_TYPE          = Type.getType(MethodType.class);
    static final Type TYPE_METHOD_HANDLE        = Type.getType(MethodHandle.class);
    static final Type TYPE_LOOKUP               = Type.getType(MethodHandles.Lookup.class);
    static final Type TYPE_S_METHOD_HANDLE      = Type.getType(SMethodHandle.class);

    static final String CLINIT = "<clinit>";
    static final String INIT = "<init>";
    static final String DEFAULT_INIT_DESC = Type.getMethodDescriptor(Type.VOID_TYPE);

    static final String METHODHANDLE_TYPE = TYPE_METHODHANDLE.getInternalName();
    static final String SPECIALIZATION_TYPE = TYPE_SPECIALIZATION.getInternalName();
    static final String SPECIALIZATION_INIT2 = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_METHODHANDLE, Type.getType(boolean.class));
    static final String SPECIALIZATION_INIT3 = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_METHODHANDLE, TYPE_CLASS, Type.getType(boolean.class));
    static final String OBJECT_TYPE = TYPE_OBJECT.getInternalName();
    static final String OBJECT_DESC = TYPE_OBJECT.getDescriptor();
    static final String STRING_TYPE = TYPE_STRING.getInternalName();
    static final String STRING_DESC = TYPE_STRING.getDescriptor();
    static final String OBJECT_ARRAY_DESC = Type.getDescriptor(Object[].class);
    static final String ARRAYLIST_TYPE = TYPE_ARRAYLIST.getInternalName();
    static final String COLLECTION_TYPE = TYPE_COLLECTION.getInternalName();
    static final String COLLECTIONS_TYPE = TYPE_COLLECTIONS.getInternalName();

    // java.util.Collection.add(Object)
    static final String COLLECTION_ADD = "add";
    static final String COLLECTION_ADD_DESC = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, TYPE_OBJECT);
    // java.util.ArrayList.<init>(int)
    static final String ARRAYLIST_INIT_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE);
    // java.util.Collections.EMPTY_LIST
    static final String COLLECTIONS_EMPTY_LIST = "EMPTY_LIST";
    static final String LIST_DESC = TYPE_LIST.getDescriptor();

    // Nashorn types, methods
    static final Type TYPE_ACCESSORPROPERTY   = Type.getType(AccessorProperty.class);
    static final Type TYPE_PROPERTYMAP        = Type.getType(PropertyMap.class);
    static final Type TYPE_PROTOTYPEOBJECT    = Type.getType(PrototypeObject.class);
    static final Type TYPE_SCRIPTFUNCTION     = Type.getType(ScriptFunction.class);
    static final Type TYPE_SCRIPTOBJECT       = Type.getType(ScriptObject.class);

    static final String PROTOTYPE_SUFFIX = "$Prototype";
    static final String CONSTRUCTOR_SUFFIX = "$Constructor";

    // This field name is known to Nashorn runtime (Context).
    // Synchronize the name change, if needed at all.
    static final String PROPERTYMAP_FIELD_NAME = "$nasgenmap$";
    static final String $CLINIT$ = "$clinit$";

    // AccessorProperty
    static final String ACCESSORPROPERTY_TYPE = TYPE_ACCESSORPROPERTY.getInternalName();
    static final String ACCESSORPROPERTY_CREATE = "create";
    static final String ACCESSORPROPERTY_CREATE_DESC =
        Type.getMethodDescriptor(TYPE_ACCESSORPROPERTY, TYPE_STRING, Type.INT_TYPE, TYPE_METHODHANDLE, TYPE_METHODHANDLE);

    // PropertyMap
    static final String PROPERTYMAP_TYPE = TYPE_PROPERTYMAP.getInternalName();
    static final String PROPERTYMAP_DESC = TYPE_PROPERTYMAP.getDescriptor();
    static final String PROPERTYMAP_NEWMAP = "newMap";
    static final String PROPERTYMAP_NEWMAP_DESC = Type.getMethodDescriptor(TYPE_PROPERTYMAP, TYPE_COLLECTION);

    // PrototypeObject
    static final String PROTOTYPEOBJECT_TYPE = TYPE_PROTOTYPEOBJECT.getInternalName();
    static final String PROTOTYPEOBJECT_SETCONSTRUCTOR = "setConstructor";
    static final String PROTOTYPEOBJECT_SETCONSTRUCTOR_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, TYPE_OBJECT);

    // ScriptFunction
    static final String SCRIPTFUNCTION_TYPE = TYPE_SCRIPTFUNCTION.getInternalName();
    static final String SCRIPTFUNCTION_SETARITY = "setArity";
    static final String SCRIPTFUNCTION_SETARITY_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE);
    static final String SCRIPTFUNCTION_SETPROTOTYPE = "setPrototype";
    static final String SCRIPTFUNCTION_SETPROTOTYPE_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT);
    static final String SCRIPTFUNCTION_CREATEBUILTIN = "createBuiltin";
    static final String SCRIPTFUNCTION_CREATEBUILTIN_DESC =
        Type.getMethodDescriptor(TYPE_SCRIPTFUNCTION, TYPE_STRING, TYPE_METHODHANDLE);
    static final String SCRIPTFUNCTION_CREATEBUILTIN_SPECS_DESC =
        Type.getMethodDescriptor(TYPE_SCRIPTFUNCTION, TYPE_STRING, TYPE_METHODHANDLE, TYPE_SPECIALIZATION_ARRAY);
    static final String SCRIPTFUNCTION_INIT_DESC3 =
        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_STRING, TYPE_METHODHANDLE, TYPE_SPECIALIZATION_ARRAY);
    static final String SCRIPTFUNCTION_INIT_DESC4 =
        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_STRING, TYPE_METHODHANDLE, TYPE_PROPERTYMAP, TYPE_SPECIALIZATION_ARRAY);

    // ScriptObject
    static final String SCRIPTOBJECT_TYPE = TYPE_SCRIPTOBJECT.getInternalName();
    static final String SCRIPTOBJECT_DESC = TYPE_SCRIPTOBJECT.getDescriptor();
    static final String SCRIPTOBJECT_INIT_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_PROPERTYMAP);

    static final String GETTER_PREFIX = "G$";
    static final String SETTER_PREFIX = "S$";

    // ScriptObject.getClassName() method.
    static final String GET_CLASS_NAME = "getClassName";
    static final String GET_CLASS_NAME_DESC = Type.getMethodDescriptor(TYPE_STRING);

    static final Handle MAKE_DIRECT_CALL_SITE = new Handle(H_INVOKESTATIC, TYPE_S_METHOD_HANDLE.getInternalName(), 
            "wrapDirect", Type.getMethodDescriptor(TYPE_CALL_SITE, 
            TYPE_LOOKUP, TYPE_STRING, TYPE_METHOD_TYPE, Type.INT_TYPE));
    static final String MAKE_DIRECT_CALL_SITE_DESC = Type.getMethodDescriptor(TYPE_S_METHOD_HANDLE, TYPE_METHOD_HANDLE);
}
