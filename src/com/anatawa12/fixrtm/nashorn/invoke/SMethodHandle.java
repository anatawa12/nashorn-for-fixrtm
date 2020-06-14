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

import jdk.internal.org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Serializable method handle. S means Serializable
 */
public abstract class SMethodHandle implements Serializable {
    // non-final for serialization
    protected transient MethodHandle real;

    public MethodHandle getReal() {
        return real;
    }

    private SMethodHandle(MethodHandle real) {
        this.real = real;
    }


    public SMethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, arrayLength, HandleAndClassAndIntSMethodHandle.TYPE_COLLECTOR);
    }

    public SMethodHandle asVarargsCollector(Class<?> arrayType) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, 0, HandleAndClassAndIntSMethodHandle.TYPE_VARARGS_COLLECTOR);
    }

    public SMethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, arrayLength, HandleAndClassAndIntSMethodHandle.TYPE_SPREADER);
    }

    public SMethodHandle asFixedArity() {
        return new HandleAndClassAndIntSMethodHandle(this, null, 0, HandleAndClassAndIntSMethodHandle.TYPE_FIXED_ARITY);
    }

    public SMethodHandle asType(MethodType newType) {
        return new HandleAndTypeSMethodHandle(this, newType, HandleAndTypeSMethodHandle.TYPE_AS_CAST);
    }

    public SMethodHandle bindTo(Object x) {
        return SMethodHandles.insertArguments(this, 0, x);
    }

    public Object invokeWithArguments(List<?> arguments) throws Throwable {
        return getReal().invokeWithArguments(arguments);
    }

    public Object invokeWithArguments(Object... arguments) throws Throwable {
        return getReal().invokeWithArguments(arguments);
    }

    public MethodType type() {
        return getReal().type();
    }

    public boolean isVarargsCollector() {
        return getReal().isVarargsCollector();
    }

    private static final MethodType DIRECT_MAKE_TYPE = MethodType.methodType(SMethodHandle.class, MethodHandle.class);
    private static final CallSite DIRECT_FIELD = makeDirectMaker("directField");
    private static final CallSite DIRECT_METHOD = makeDirectMaker("directMethod");
    private static final CallSite DIRECT_SPECIAL_METHOD = makeDirectMaker("directSpecialMethod");
    private static final CallSite DIRECT_CONSTRUCTOR = makeDirectMaker("directConstructor");

    private static CallSite makeDirectMaker(String name) {
        try {
            return new ConstantCallSite(MethodHandles.lookup().findStatic(SMethodHandle.class, name, DIRECT_MAKE_TYPE));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static CallSite wrapDirect(MethodHandles.Lookup caller, String name, MethodType type, int ref) {
        switch (ref) {
            case Opcodes.H_GETFIELD:
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTFIELD:
            case Opcodes.H_PUTSTATIC:
                return DIRECT_FIELD;
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKESTATIC:
            case Opcodes.H_INVOKEINTERFACE:
                return DIRECT_METHOD;
            case Opcodes.H_INVOKESPECIAL:
                return DIRECT_SPECIAL_METHOD;
            case Opcodes.H_NEWINVOKESPECIAL:
                return DIRECT_CONSTRUCTOR;
            default:
                throw new IllegalArgumentException("ref type is not valid");
        }
    }

    static SMethodHandle directMethod(MethodHandle real) {
        return new DirectMethodSMethodHandle(real, false);
    }

    static SMethodHandle directSpecialMethod(MethodHandle real) {
        return new DirectMethodSMethodHandle(real, true);
    }

    static SMethodHandle directConstructor(MethodHandle real) {
        return new DirectConstructorSMethodHandle(real);
    }

    static SMethodHandle directField(MethodHandle real) {
        return new DirectFieldSMethodHandle(real);
    }

    SMethodHandle permuteArgumentsInternal(MethodType newType, int[] reorder) {
        return new PermuteArgumentsSMethodHandle(this, newType, reorder);
    }

    private final static class DirectMethodSMethodHandle extends SMethodHandle {
        boolean isSpecial;

        DirectMethodSMethodHandle(MethodHandle real, boolean isSpecial) {
            super(real);
            // check is direct method method handle.
            MethodHandles.reflectAs(Method.class, real);
            this.isSpecial = isSpecial;
        }

        private void readObject(java.io.ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }

        private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
            throw new InvalidObjectException("Proxy required");
        }

        private Object writeReplace() throws NotSerializableException {
            return new SerializationProxy(this);
        }

        private static class SerializationProxy implements Serializable {
            private final Class<?> owner;
            private final String name;
            private final Class<?>[] parameters;
            private final Class<?> result;

            SerializationProxy(DirectMethodSMethodHandle handle) throws NotSerializableException {
                Method method = MethodHandles.reflectAs(Method.class, handle.getReal());
                if (handle.isSpecial && !Modifier.isPrivate(method.getModifiers())) {
                    throw new NotSerializableException("special invocation to non-private method is not serializable.");
                }
                owner = method.getDeclaringClass();
                name = method.getName();
                parameters = method.getParameterTypes();
                result = method.getReturnType();
            }

            private Object readResolve() throws NoSuchMethodException, IllegalAccessException {
                Method method = null;
                for (Method declaredMethod : owner.getDeclaredMethods()) {
                    if (declaredMethod.getName().equals(name)
                            && Arrays.equals(declaredMethod.getGenericParameterTypes(), parameters)
                            && declaredMethod.getReturnType() == result) {
                        method = declaredMethod;
                        break;
                    }
                }
                if (method == null)
                    throw new NoSuchMethodException(toString());
                method.setAccessible(true);
                return new DirectMethodSMethodHandle(MethodHandles.publicLookup().unreflect(method), true);
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append(owner.getName())
                        .append('.')
                        .append(name)
                        .append('(');
                for (int i = 0; i < parameters.length; i++) {
                    if (i != 0)
                        builder.append(',');
                    builder.append(parameters[i].getName());
                }
                builder.append(')').append(result.getName());
                return builder.toString();
            }
        }
    }

    private final static class DirectConstructorSMethodHandle extends SMethodHandle {
        DirectConstructorSMethodHandle(MethodHandle real) {
            super(real);
            // check is direct method method handle.
            MethodHandles.reflectAs(Constructor.class, real);
        }

        private void readObject(java.io.ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }

        private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
            throw new InvalidObjectException("Proxy required");
        }

        private Object writeReplace() {
            return new SerializationProxy(this);
        }

        private static class SerializationProxy implements Serializable {
            private final Class<?> owner;
            private final Class<?>[] parameters;

            SerializationProxy(DirectConstructorSMethodHandle handle) {
                Constructor<?> Constructor = MethodHandles.reflectAs(Constructor.class, handle.getReal());
                owner = Constructor.getDeclaringClass();
                parameters = Constructor.getParameterTypes();
            }

            private Object readResolve() throws NoSuchMethodException, IllegalAccessException {
                Constructor<?> constructor = null;
                for (Constructor<?> declaredConstructor : owner.getDeclaredConstructors()) {
                    if (Arrays.equals(declaredConstructor.getGenericParameterTypes(), parameters)) {
                        constructor = declaredConstructor;
                        break;
                    }
                }
                if (constructor == null)
                    throw new NoSuchMethodException(toString());
                constructor.setAccessible(true);
                return new DirectConstructorSMethodHandle(MethodHandles.publicLookup().unreflectConstructor(constructor));
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append(owner.getName())
                        .append(".<init>(");
                for (int i = 0; i < parameters.length; i++) {
                    if (i != 0)
                        builder.append(',');
                    builder.append(parameters[i].getName());
                }
                builder.append(')');
                return builder.toString();
            }
        }
    }

    private final static class DirectFieldSMethodHandle extends SMethodHandle {
        DirectFieldSMethodHandle(MethodHandle real) {
            super(real);
            // check is direct field method handle.
            MethodHandles.reflectAs(Field.class, real);
        }

        private void readObject(java.io.ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }

        private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
            throw new InvalidObjectException("Proxy required");
        }

        private Object writeReplace() {
            return new SerializationProxy(this);
        }

        private static class SerializationProxy implements Serializable {
            private final Class<?> owner;
            private final String name;
            private final Class<?> type;
            protected final boolean isGet;

            SerializationProxy(DirectFieldSMethodHandle handle) {
                Field field = MethodHandles.reflectAs(Field.class, handle.getReal());
                owner = field.getDeclaringClass();
                name = field.getName();
                type = field.getType();
                // if setter, will return void.
                isGet = handle.getReal().type().returnType() != void.class;
            }

            protected Field getField() throws NoSuchFieldException {
                for (Field field : owner.getFields()) {
                    if (field.getName().equals(name)
                            && field.getType() == type)
                        return field;
                }
                throw new NoSuchFieldException(toString());
            }

            private Object readResolve() throws IllegalAccessException, NoSuchFieldException {
                Field field = getField();
                field.setAccessible(true);
                MethodHandle handle;
                if (isGet) handle = MethodHandles.publicLookup().unreflectGetter(field);
                else handle = MethodHandles.publicLookup().unreflectSetter(field);
                return new DirectFieldSMethodHandle(handle);
            }

            @Override
            public String toString() {
                return owner.getName() + '.' + name + '/' + type + '/' + (isGet ? "get" : "set");
            }
        }
    }

    private static final int VALUE_MASK = 0x00FFFFFF;
    private static final int TYPE_MASK = 0xFF000000;

    // for HandleAndClassAndIntSMethodHandle
    public static final int TYPE_COLLECTOR = 0x00000000;
    public static final int TYPE_VARARGS_COLLECTOR = 0x01000000;
    public static final int TYPE_SPREADER = 0x02000000;
    public static final int TYPE_FIXED_ARITY = 0x03000000;
    public static final int TYPE_ARRAY_GETTER = 0x04000000;
    public static final int TYPE_ARRAY_SETTER = 0x05000000;
    public static final int TYPE_IDENTITY = 0x06000000;

    // for HandleAndTypeSMethodHandle
    public static final int TYPE_AS_CAST = 0x00000000;
    public static final int TYPE_EXPLICIT_CAST = 0x01000000;
    public static final int TYPE_INVOKER = 0x02000000;
    public static final int TYPE_EXACT_INVOKER = 0x03000000;
    public static final int TYPE_SPREAD_INVOKER = 0x04000000;

    // for HandlesSMethodHandle
    public static final int TYPE_FILTER_ARGUMENTS = 0x00000000;
    public static final int TYPE_COLLECT_ARGUMENTS = 0x01000000;
    public static final int TYPE_FILTER_RETURN = 0x02000000;
    public static final int TYPE_FOLD_ARGUMENTS = 0x03000000;
    public static final int TYPE_GUARD_WITH_TEST = 0x04000000;

    final static class HandleAndClassAndIntSMethodHandle extends SMethodHandle {
        private final SMethodHandle base;
        private final Class<?> arrayType;
        private final int lengthAndType;

        public HandleAndClassAndIntSMethodHandle(SMethodHandle base, Class<?> arrayType, int length, int type) {
            super(makeReal(base, arrayType, length, type));
            this.base = base;
            this.arrayType = arrayType;
            this.lengthAndType = length | type;
        }

        private Object readResolve() {
            return new HandleAndClassAndIntSMethodHandle(base, arrayType, lengthAndType & VALUE_MASK, lengthAndType & TYPE_MASK);
        }

        private static MethodHandle makeReal(SMethodHandle base, Class<?> arrayType, int length, int type) {
            MethodHandle real;
            switch (type) {
                case TYPE_COLLECTOR:
                    real = base.getReal().asCollector(arrayType, length);
                    break;
                case TYPE_VARARGS_COLLECTOR:
                    real = base.getReal().asVarargsCollector(arrayType);
                    length = 0;
                    break;
                case TYPE_SPREADER:
                    real = base.getReal().asSpreader(arrayType, length);
                    break;
                case TYPE_FIXED_ARITY:
                    real = base.getReal().asFixedArity();
                    length = 0;
                    break;
                case TYPE_ARRAY_GETTER:
                    real = MethodHandles.arrayElementGetter(arrayType);
                    length = 0;
                    break;
                case TYPE_ARRAY_SETTER:
                    real = MethodHandles.arrayElementSetter(arrayType);
                    length = 0;
                    break;
                case TYPE_IDENTITY:
                    real = MethodHandles.identity(arrayType);
                    length = 0;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            if ((length & 0xFF) != length) throw new IllegalArgumentException("length must be le 255. : " + length);
            return real;
        }
    }

    final static class HandleAndTypeSMethodHandle extends SMethodHandle {
        private final SMethodHandle base;
        private final MethodType methodType;
        private final int type;


        public HandleAndTypeSMethodHandle(SMethodHandle base, MethodType methodType, int type) {
            super(makeReal(base, methodType, 0, type));
            this.base = base;
            this.methodType = methodType;
            this.type = type;
        }

        public HandleAndTypeSMethodHandle(SMethodHandle base, MethodType methodType, int value, int type) {
            super(makeReal(base, methodType, value, type));
            this.base = base;
            this.methodType = methodType;
            this.type = type;
        }

        private Object readResolve() {
            return new HandleAndTypeSMethodHandle(base, methodType, type & VALUE_MASK, type & TYPE_MASK);
        }

        private static MethodHandle makeReal(SMethodHandle base, MethodType methodType, int value, int type) {
            MethodHandle real;
            switch (type) {
                case TYPE_AS_CAST:
                    real = base.getReal().asType(methodType);
                    break;
                case TYPE_EXPLICIT_CAST:
                    real = MethodHandles.explicitCastArguments(base.getReal(), methodType);
                    break;
                case TYPE_INVOKER:
                    real = MethodHandles.invoker(methodType);
                    break;
                case TYPE_EXACT_INVOKER:
                    real = MethodHandles.exactInvoker(methodType);
                    break;
                case TYPE_SPREAD_INVOKER:
                    real = MethodHandles.spreadInvoker(methodType, value);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            return real;
        }
    }

    final static class HandlesSMethodHandle extends SMethodHandle {
        private final SMethodHandle first;
        private final SMethodHandle[] handles;
        private final int type;


        public HandlesSMethodHandle(int value, int type, SMethodHandle first, SMethodHandle... handles) {
            super(makeReal(first, handles, value, type));
            this.first = first;
            this.handles = handles.clone();
            this.type = type;
        }

        private Object readResolve() {
            return new HandlesSMethodHandle(type & VALUE_MASK, type & TYPE_MASK, first, handles);
        }

        private static MethodHandle makeReal(SMethodHandle first, SMethodHandle[] handles, int value, int type) {
            MethodHandle real;
            switch (type) {
                case TYPE_FILTER_ARGUMENTS:
                    MethodHandle[] reals;
                    reals = new MethodHandle[handles.length];
                    for (int i = 0; i < reals.length; i++) {
                        reals[i] = handles[i].getReal();
                    }
                    real = MethodHandles.filterArguments(first.getReal(), value, reals);
                    break;
                case TYPE_COLLECT_ARGUMENTS:
                    real = MethodHandles.collectArguments(first.getReal(), value, handles[0].getReal());
                    break;
                case TYPE_FILTER_RETURN:
                    real = MethodHandles.filterReturnValue(first.getReal(), handles[0].getReal());
                    break;
                case TYPE_FOLD_ARGUMENTS:
                    real = MethodHandles.foldArguments(first.getReal(), handles[0].getReal());
                    break;
                case TYPE_GUARD_WITH_TEST:
                    real = MethodHandles.guardWithTest(first.getReal(), handles[0].getReal(), handles[1].getReal());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            return real;
        }
    }

    final static class ConstantSMethodHandle extends SMethodHandle {
        private final Class<?> type;
        private final Object value;

        public ConstantSMethodHandle(Class<?> type, Object value) {
            super(MethodHandles.constant(type, value));
            this.type = type;
            this.value = value;
        }

        private Object readResolve() {
            return new ConstantSMethodHandle(type, value);
        }
    }

    final static class InsertArgumentsSMethodHandle extends SMethodHandle {
        private final SMethodHandle target;
        private final int pos;
        private final Object[] values;

        public InsertArgumentsSMethodHandle(SMethodHandle target, int pos, Object[] values) {
            super(MethodHandles.insertArguments(target.getReal(), pos, values));
            this.target = target;
            this.pos = pos;
            this.values = values.clone();
        }

        private Object readResolve() {
            return new InsertArgumentsSMethodHandle(target, pos, values);
        }
    }

    final static class CatchExceptionSMethodHandle extends SMethodHandle {
        private final SMethodHandle target;
        private final Class<? extends Throwable> exceptionType;
        private final SMethodHandle handle;

        public CatchExceptionSMethodHandle(SMethodHandle target, Class<? extends Throwable> exceptionType, SMethodHandle handle) {
            super(MethodHandles.catchException(target.real, exceptionType, handle.real));
            this.target = target;
            this.exceptionType = exceptionType;
            this.handle = handle;
        }

        private Object readResolve() {
            return new CatchExceptionSMethodHandle(target, exceptionType, handle);
        }
    }

    final static class ThrowSMethodHandle extends SMethodHandle {
        private final Class<?> returnType;
        private final Class<? extends Throwable> exceptionType;

        public ThrowSMethodHandle(Class<?> returnType, Class<? extends Throwable> exceptionType) {
            super(MethodHandles.throwException(returnType, exceptionType));
            this.returnType = returnType;
            this.exceptionType = exceptionType;
        }

        private Object readResolve() {
            return new ThrowSMethodHandle(returnType, exceptionType);
        }
    }

    final static class PermuteArgumentsSMethodHandle extends SMethodHandle {
        private SMethodHandle target;
        private MethodType newType;
        private int[] reorder;

        public PermuteArgumentsSMethodHandle(SMethodHandle target, MethodType newType, int[] reorder) {
            super(MethodHandles.permuteArguments(target.real, newType, reorder));
            this.target = target;
            this.newType = newType;
            this.reorder = reorder;
        }

        @Override
        SMethodHandle permuteArgumentsInternal(MethodType newType, int[] reorder) {
            if (reorder.length != this.newType.parameterCount())
                throw new IllegalArgumentException("bad reorder array: " + Arrays.toString(reorder));
            int[] newReorder = new int[this.reorder.length];

            for (int i = 0; i < newReorder.length; i++) {
                newReorder[i] = reorder[this.reorder[i]];
            }

            return new PermuteArgumentsSMethodHandle(target, newType, newReorder);
        }

        private Object readResolve() {
            return new PermuteArgumentsSMethodHandle(target, newType, reorder.clone());
        }
    }
}
