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
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.List;
import java.util.Arrays;

/**
 * @see MethodHandles
 */
public class SMethodHandles {
    private SMethodHandles() {
    }  // do not instantiate

    /* can't wrap caller sensitive method.
     * 
     * @see MethodHandles#lookup() 
     * /
    @CallerSensitive
    public static SMethodHandles.Lookup lookup() {
        return new SMethodHandles.Lookup(Reflection.getCallerClass());
    }
     */

    /**
     * wraps real to {@link SMethodHandles.Lookup}
     *
     * @param real Lookup to wrap
     * @return wrapped Lookup
     */
    public static SMethodHandles.Lookup l(MethodHandles.Lookup real) {
        return new SMethodHandles.Lookup(real);
    }

    /**
     * @see MethodHandles#publicLookup()
     */
    public static SMethodHandles.Lookup publicLookup() {
        return Lookup.PUBLIC_LOOKUP;
    }

    /**
     * @see MethodHandles#reflectAs(Class, MethodHandle)
     */
    public static <T extends Member> T
    reflectAs(Class<T> expected, SMethodHandle target) {
        return MethodHandles.reflectAs(expected, target.getReal());
    }

    /**
     * a wrapper of {@link SMethodHandles.Lookup} for {@link SMethodHandle}
     *
     * @see MethodHandles.Lookup
     */
    public static final class Lookup {
        private static final Lookup PUBLIC_LOOKUP = l(MethodHandles.publicLookup());
        private final MethodHandles.Lookup real;

        /**
         * @see MethodHandles.Lookup#PUBLIC
         */
        public static final int PUBLIC = MethodHandles.Lookup.PUBLIC;

        /**
         * @see MethodHandles.Lookup#PRIVATE
         */
        public static final int PRIVATE = MethodHandles.Lookup.PRIVATE;

        /**
         * @see MethodHandles.Lookup#PROTECTED
         */
        public static final int PROTECTED = MethodHandles.Lookup.PROTECTED;

        /**
         * @see MethodHandles.Lookup#PACKAGE
         */
        public static final int PACKAGE = MethodHandles.Lookup.PACKAGE;

        /**
         * @see MethodHandles.Lookup#lookupClass()
         */
        public Class<?> lookupClass() {
            return real.lookupClass();
        }

        /**
         * @see MethodHandles.Lookup#lookupModes()
         */
        public int lookupModes() {
            return real.lookupModes();
        }

        /**
         * wraps {@link SMethodHandles.Lookup}
         */
        Lookup(MethodHandles.Lookup real) {
            this.real = real;
        }

        /**
         * @see MethodHandles.Lookup#in(Class)
         */
        public SMethodHandles.Lookup in(Class<?> requestedLookupClass) {
            return new SMethodHandles.Lookup(real.in(requestedLookupClass));
        }

        /**
         * @see MethodHandles.Lookup#toString()
         */
        @Override
        public String toString() {
            return real.toString();
        }

        /**
         * @see MethodHandles.Lookup#findStatic(Class, String, MethodType)
         */
        public SMethodHandle findStatic(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            return SMethodHandle.directMethod(real.findStatic(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#findVirtual(Class, String, MethodType)
         */
        public SMethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            return SMethodHandle.directMethod(real.findVirtual(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#findConstructor(Class, MethodType)
         */
        public SMethodHandle findConstructor(Class<?> refc, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            return SMethodHandle.directMethod(real.findConstructor(refc, type));
        }

        /**
         * @see MethodHandles.Lookup#findSpecial(Class, String, MethodType, Class)
         */
        public SMethodHandle findSpecial(Class<?> refc, String name, MethodType type,
                                         Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
            return SMethodHandle.directSpecialMethod(real.findSpecial(refc, name, type, specialCaller));
        }

        /**
         * @see MethodHandles.Lookup#findGetter(Class, String, Class)
         */
        public SMethodHandle findGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return SMethodHandle.directField(real.findGetter(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#findSetter(Class, String, Class)
         */
        public SMethodHandle findSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return SMethodHandle.directField(real.findSetter(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#findStaticGetter(Class, String, Class)
         */
        public SMethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return SMethodHandle.directField(real.findStaticGetter(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#findStaticSetter(Class, String, Class)
         */
        public SMethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return SMethodHandle.directField(real.findStaticSetter(refc, name, type));
        }

        /**
         * @see MethodHandles.Lookup#bind(Object, String, MethodType)
         */
        public SMethodHandle bind(Object receiver, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            return null;//TODO: return real.bind(receiver, name, type);
        }

        /**
         * @see MethodHandles.Lookup#unreflect(Method)
         */
        public SMethodHandle unreflect(Method m) throws IllegalAccessException {
            return SMethodHandle.directMethod(real.unreflect(m));
        }

        /* can't make special methods because no way to deserialize.
         * @see MethodHandles.Lookup#unreflectSpecial(Method, Class) 
         * /
        public SMethodHandle unreflectSpecial(Method m, Class<?> specialCaller) throws IllegalAccessException {
            return SMethodHandle.directMethod(real.unreflectSpecial(m, specialCaller));
        }
        // */

        /**
         * @see MethodHandles.Lookup#unreflectConstructor(Constructor)
         */
        public SMethodHandle unreflectConstructor(Constructor<?> c) throws IllegalAccessException {
            return SMethodHandle.directMethod(real.unreflectConstructor(c));
        }

        /**
         * @see MethodHandles.Lookup#unreflectGetter(Field)
         */
        public SMethodHandle unreflectGetter(Field f) throws IllegalAccessException {
            return SMethodHandle.directField(real.unreflectGetter(f));
        }

        /**
         * @see MethodHandles.Lookup#unreflectSetter(Field)
         */
        public SMethodHandle unreflectSetter(Field f) throws IllegalAccessException {
            return SMethodHandle.directField(real.unreflectSetter(f));
        }

        /**
         * @see MethodHandles.Lookup#revealDirect(MethodHandle)
         */
        public MethodHandleInfo revealDirect(SMethodHandle target) {
            //TODO: wrap?
            return real.revealDirect(target.getReal());
        }
    }

    /**
     * @see MethodHandles#arrayElementGetter(Class)
     */
    public static SMethodHandle arrayElementGetter(Class<?> arrayClass) throws IllegalArgumentException {
        Class<?> elementClass = arrayClass.getComponentType();
        if (elementClass == null) throw new IllegalArgumentException("not an array: " + arrayClass);
        return new SMethodHandle.HandleAndClassAndIntSMethodHandle(null, 
                arrayClass, 0, 
                SMethodHandle.TYPE_ARRAY_GETTER);
    }

    /**
     * @see MethodHandles#arrayElementSetter(Class)
     */
    public static SMethodHandle arrayElementSetter(Class<?> arrayClass) throws IllegalArgumentException {
        Class<?> elementClass = arrayClass.getComponentType();
        if (elementClass == null) throw new IllegalArgumentException("not an array: " + arrayClass);
        return new SMethodHandle.HandleAndClassAndIntSMethodHandle(null,
                arrayClass, 0, 
                SMethodHandle.TYPE_ARRAY_SETTER);
    }

    /// method handle invocation (reflective style)

    /**
     * @see MethodHandles#spreadInvoker(MethodType, int)
     */
    static public SMethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        return new SMethodHandle.HandleAndTypeSMethodHandle(null, type, leadingArgCount, SMethodHandle.TYPE_SPREAD_INVOKER);
    }

    /**
     * @see MethodHandles#exactInvoker(MethodType)
     */
    static public SMethodHandle exactInvoker(MethodType type) {
        return new SMethodHandle.HandleAndTypeSMethodHandle(null, type, SMethodHandle.TYPE_EXACT_INVOKER);
    }

    /**
     * @see MethodHandles#invoker(MethodType)
     */
    static public SMethodHandle invoker(MethodType type) {
        return new SMethodHandle.HandleAndTypeSMethodHandle(null, type, SMethodHandle.TYPE_INVOKER);
    }

    /// method handle modification (creation from other method handles)

    /**
     * @see MethodHandles#explicitCastArguments(MethodHandle, MethodType)
     */
    public static SMethodHandle explicitCastArguments(SMethodHandle target, MethodType newType) {
        return new SMethodHandle.HandleAndTypeSMethodHandle(target, newType, SMethodHandle.TYPE_EXPLICIT_CAST);
    }

    /**
     * @see MethodHandles#permuteArguments(MethodHandle, MethodType, int...)
     */
    public static SMethodHandle permuteArguments(SMethodHandle target, MethodType newType, int... reorder) {
        return target.permuteArgumentsInternal(newType, reorder);
    }

    /**
     * @see MethodHandles#constant(Class, Object)
     */
    public static SMethodHandle constant(Class<?> type, Object value) {
        return new SMethodHandle.ConstantSMethodHandle(type, value);
    }

    /**
     * @see MethodHandles#identity(Class)
     */
    public static SMethodHandle identity(Class<?> type) {
        return new SMethodHandle.HandleAndClassAndIntSMethodHandle(null, type, 0, SMethodHandle.HandleAndClassAndIntSMethodHandle.TYPE_IDENTITY);
    }

    /**
     * @see MethodHandles#insertArguments(MethodHandle, int, Object...)
     */
    public static SMethodHandle insertArguments(SMethodHandle target, int pos, Object... values) {
        return new SMethodHandle.InsertArgumentsSMethodHandle(target, pos, values);
    }

    /**
     * @see MethodHandles#dropArguments(MethodHandle, int, List)
     */
    public static SMethodHandle dropArguments(SMethodHandle target, int pos, List<Class<?>> valueTypes) {
        MethodType type = target.type().insertParameterTypes(pos, valueTypes);

        if (pos < 0 || pos > target.type().parameterCount())
            throw new IllegalArgumentException("invalid pos.");

        int[] droppedArguments = new int[target.type().parameterCount()];
        int i = 0;
        int j = 0;
        for (; i < pos; i++, j++) {
            droppedArguments[i] = j;
        }
        j += valueTypes.size();
        for (; i < droppedArguments.length; i++, j++) {
            droppedArguments[i] = j;
        }
        assert j == type.parameterCount();
        return permuteArguments(target, type, droppedArguments);
    }

    /**
     * @see MethodHandles#dropArguments(MethodHandle, int, Class[])
     */
    public static SMethodHandle dropArguments(SMethodHandle target, int pos, Class<?>... valueTypes) {
        return dropArguments(target, pos, Arrays.asList(valueTypes));
    }

    /**
     * @see MethodHandles#filterArguments(MethodHandle, int, MethodHandle...)
     */
    public static SMethodHandle filterArguments(SMethodHandle target, int pos, SMethodHandle... filters) {
        return new SMethodHandle.HandlesSMethodHandle(pos, SMethodHandle.TYPE_FILTER_ARGUMENTS, target, filters);
    }

    /**
     * @see MethodHandles#collectArguments(MethodHandle, int, MethodHandle)
     */
    public static SMethodHandle collectArguments(SMethodHandle target, int pos, SMethodHandle filter) {
        return new SMethodHandle.HandlesSMethodHandle(pos, SMethodHandle.TYPE_COLLECT_ARGUMENTS, target, filter);
    }

    /**
     * @see MethodHandles#filterReturnValue(MethodHandle, MethodHandle)
     */
    public static SMethodHandle filterReturnValue(SMethodHandle target, SMethodHandle filter) {
        return new SMethodHandle.HandlesSMethodHandle(0, SMethodHandle.TYPE_FILTER_RETURN, target, filter);
    }

    /**
     * @see MethodHandles#foldArguments(MethodHandle, MethodHandle)
     */
    public static SMethodHandle foldArguments(SMethodHandle target, SMethodHandle combiner) {
        return new SMethodHandle.HandlesSMethodHandle(0, SMethodHandle.TYPE_FOLD_ARGUMENTS, target, combiner);
    }

    /**
     * @see MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)
     */
    public static SMethodHandle guardWithTest(SMethodHandle test,
                                              SMethodHandle target,
                                              SMethodHandle fallback) {
        return new SMethodHandle.HandlesSMethodHandle(0, SMethodHandle.TYPE_GUARD_WITH_TEST, test, target, fallback);
    }

    /**
     * @see MethodHandles#catchException(MethodHandle, Class, MethodHandle)
     */
    public static SMethodHandle catchException(SMethodHandle target,
                                               Class<? extends Throwable> exceptionType,
                                               SMethodHandle handler) {
        return new SMethodHandle.CatchExceptionSMethodHandle(target, exceptionType, handler);
    }

    /**
     * @see MethodHandles#throwException(Class, Class)
     */
    public static SMethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exceptionType) {
        return new SMethodHandle.ThrowSMethodHandle(returnType, exceptionType);
    }
}
