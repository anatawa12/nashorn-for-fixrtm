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

package com.anatawa12.fixrtm.nashorn.internal.lookup;

import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle;
import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles;
import java.lang.invoke.MethodType;
import com.anatawa12.fixrtm.nashorn.invoke.SSwitchPoint;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Wrapper for all method handle related functions used in Nashorn. This interface only exists
 * so that instrumentation can be added to all method handle operations.
 */

public interface MethodHandleFunctionality {
    /**
     * Wrapper for {@link SMethodHandles#filterArguments(SMethodHandle, int, SMethodHandle...)}
     *
     * @param target  target method handle
     * @param pos     start argument index
     * @param filters filters
     *
     * @return filtered handle
     */
    public SMethodHandle filterArguments(SMethodHandle target, int pos, SMethodHandle... filters);

    /**
     * Wrapper for {@link SMethodHandles#filterReturnValue(SMethodHandle, SMethodHandle)}
     *
     * @param target  target method handle
     * @param filter  filter
     *
     * @return filtered handle
     */
    public SMethodHandle filterReturnValue(SMethodHandle target, SMethodHandle filter);

    /**
     * Wrapper for {@link SMethodHandles#guardWithTest(SMethodHandle, SMethodHandle, SMethodHandle)}
     *
     * @param test     test method handle
     * @param target   target method handle when test is true
     * @param fallback fallback method handle when test is false
     *
     * @return guarded handles
     */
    public SMethodHandle guardWithTest(SMethodHandle test, SMethodHandle target, SMethodHandle fallback);

    /**
     * Wrapper for {@link SMethodHandles#insertArguments(SMethodHandle, int, Object...)}
     *
     * @param target target method handle
     * @param pos    start argument index
     * @param values values to insert
     *
     * @return handle with bound arguments
     */
    public SMethodHandle insertArguments(SMethodHandle target, int pos, Object... values);

    /**
     * Wrapper for {@link SMethodHandles#dropArguments(SMethodHandle, int, Class...)}
     *
     * @param target     target method handle
     * @param pos        start argument index
     * @param valueTypes valueTypes of arguments to drop
     *
     * @return handle with dropped arguments
     */
    public SMethodHandle dropArguments(SMethodHandle target, int pos, Class<?>... valueTypes);

    /**
     * Wrapper for {@link SMethodHandles#dropArguments(SMethodHandle, int, List)}
     *
     * @param target     target method handle
     * @param pos        start argument index
     * @param valueTypes valueTypes of arguments to drop
     *
     * @return handle with dropped arguments
     */
    public SMethodHandle dropArguments(final SMethodHandle target, final int pos, final List<Class<?>> valueTypes);

    /**
     * Wrapper for {@link SMethodHandles#foldArguments(SMethodHandle, SMethodHandle)}
     *
     * @param target   target method handle
     * @param combiner combiner to apply for fold
     *
     * @return folded method handle
     */
    public SMethodHandle foldArguments(SMethodHandle target, SMethodHandle combiner);

    /**
     * Wrapper for {@link SMethodHandles#explicitCastArguments(SMethodHandle, MethodType)}
     *
     * @param target  target method handle
     * @param type    type to cast to
     *
     * @return modified method handle
     */
    public SMethodHandle explicitCastArguments(SMethodHandle target, MethodType type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#arrayElementGetter(Class)}
     *
     * @param arrayClass class for array
     *
     * @return array element getter
     */
    public SMethodHandle arrayElementGetter(Class<?> arrayClass);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#arrayElementSetter(Class)}
     *
     * @param arrayClass class for array
     *
     * @return array element setter
     */
    public SMethodHandle arrayElementSetter(Class<?> arrayClass);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#throwException(Class, Class)}
     *
     * @param returnType ignored, but method signature will use it
     * @param exType     exception type that will be thrown
     *
     * @return exception thrower method handle
     */
    public SMethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#catchException(SMethodHandle, Class, SMethodHandle)}
     *
     * @param target  target method
     * @param exType  exception type
     * @param handler the method handle to call when exception is thrown
     *
     * @return exception thrower method handle
     */
    public SMethodHandle catchException(final SMethodHandle target, final Class<? extends Throwable> exType, final SMethodHandle handler);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#constant(Class, Object)}
     *
     * @param type  type of constant
     * @param value constant value
     *
     * @return method handle that returns said constant
     */
    public SMethodHandle constant(Class<?> type, Object value);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles#identity(Class)}
     *
     * @param type  type of value
     *
     * @return method handle that returns identity argument
     */
    public SMethodHandle identity(Class<?> type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle#asType(MethodType)}
     *
     * @param handle  method handle for type conversion
     * @param type    type to convert to
     *
     * @return method handle with given type conversion applied
     */
    public SMethodHandle asType(SMethodHandle handle, MethodType type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle#asCollector(Class, int)}
     *
     * @param handle      handle to convert
     * @param arrayType   array type for collector array
     * @param arrayLength length of collector array
     *
     * @return method handle with collector
     */
    public SMethodHandle asCollector(SMethodHandle handle, Class<?> arrayType, int arrayLength);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle#asSpreader(Class, int)}
     *
     * @param handle      handle to convert
     * @param arrayType   array type for spread
     * @param arrayLength length of spreader
     *
     * @return method handle as spreader
     */
    public SMethodHandle asSpreader(SMethodHandle handle, Class<?> arrayType, int arrayLength);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle#bindTo(Object)}
     *
     * @param handle a handle to which to bind a receiver
     * @param x      the receiver
     *
     * @return the bound handle
     */
    public SMethodHandle bindTo(SMethodHandle handle, Object x);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findGetter(Class, String, Class)}
      *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of field
     * @param type           type of field
     *
     * @return getter method handle for virtual field
     */
    public SMethodHandle getter(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, Class<?> type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findStaticGetter(Class, String, Class)}
      *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of field
     * @param type           type of field
     *
     * @return getter method handle for static field
     */
    public SMethodHandle staticGetter(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, Class<?> type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findSetter(Class, String, Class)}
      *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of field
     * @param type           type of field
     *
     * @return setter method handle for virtual field
     */
    public SMethodHandle setter(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, Class<?> type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findStaticSetter(Class, String, Class)}
      *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of field
     * @param type           type of field
     *
     * @return setter method handle for static field
     */
    public SMethodHandle staticSetter(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, Class<?> type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#unreflect(Method)}
     *
     * Unreflect a method as a method handle
     *
     * @param method method to unreflect
     * @return unreflected method as method handle
     */
    public SMethodHandle find(Method method);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findStatic(Class, String, MethodType)}
     *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of method
     * @param type           method type
     *
     * @return method handle for static method
     */
    public SMethodHandle findStatic(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, MethodType type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findVirtual(Class, String, MethodType)}
     *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of method
     * @param type           method type
     *
     * @return method handle for virtual method
     */
    public SMethodHandle findVirtual(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, MethodType type);

    /**
     * Wrapper for {@link com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles.Lookup#findSpecial(Class, String, MethodType, Class)}
     *
     * @param explicitLookup explicit lookup to be used
     * @param clazz          class to look in
     * @param name           name of method
     * @param type           method type
     * @param thisClass      thisClass
     *
     * @return method handle for virtual method
     */
    public SMethodHandle findSpecial(SMethodHandles.Lookup explicitLookup, Class<?> clazz, String name, MethodType type, final Class<?> thisClass);

    /**
     * Wrapper for SSwitchPoint creation. Just like {@code new SSwitchPoint()} but potentially
     * tracked
     *
     * @return new switch point
     */
    public SSwitchPoint createSwitchPoint();

    /**
     * Wrapper for {@link SSwitchPoint#guardWithTest(SMethodHandle, SMethodHandle)}
     *
     * @param sp     switch point
     * @param before method handle when switchpoint is valid
     * @param after  method handle when switchpoint is invalidated
     *
     * @return guarded method handle
     */
    public SMethodHandle guardWithTest(SSwitchPoint sp, SMethodHandle before, SMethodHandle after);

    /**
     * Wrapper for {@link MethodType#methodType(Class, Class...)}
     *
     * @param returnType  return type for method type
     * @param paramTypes  parameter types for method type
     *
     * @return the method type
     */
    public MethodType type(Class<?> returnType, Class<?>... paramTypes);

}

