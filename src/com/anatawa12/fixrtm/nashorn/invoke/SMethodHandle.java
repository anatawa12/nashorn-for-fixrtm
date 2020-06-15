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
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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
    protected final transient MethodHandle real;

    public MethodHandle getReal() {
        return real;
    }

    private SMethodHandle(MethodHandle real) {
        this.real = real;
    }

    public SMethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, arrayLength, HandleAndClassAndIntSMethodHandle.COLLECTOR_HANDLE);
    }

    public SMethodHandle asVarargsCollector(Class<?> arrayType) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, 0, HandleAndClassAndIntSMethodHandle.VARARGS_COLLECTOR_HANDLE);
    }

    public SMethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return new HandleAndClassAndIntSMethodHandle(this, arrayType, arrayLength, HandleAndClassAndIntSMethodHandle.SPREADER_HANDLE);
    }

    public SMethodHandle asFixedArity() {
        return new HandleAndClassAndIntSMethodHandle(this, null, 0, HandleAndClassAndIntSMethodHandle.FIXED_ARITY_HANDLE);
    }

    public SMethodHandle asType(MethodType newType) {
        return new HandleAndTypeSMethodHandle(this, newType, 0, HandleAndTypeSMethodHandle.AS_CAST_HANDLE);
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

    private static final MethodHandle GET_RIAL;
    private static final MethodType DIRECT_MAKE_TYPE = MethodType.methodType(SMethodHandle.class, MethodHandle.class);
    private static final CallSite DIRECT_FIELD = makeDirectMaker("directField");
    private static final CallSite DIRECT_METHOD = makeDirectMaker("directMethod");
    private static final CallSite DIRECT_SPECIAL_METHOD = makeDirectMaker("directSpecialMethod");
    private static final CallSite DIRECT_CONSTRUCTOR = makeDirectMaker("directConstructor");

    static {
        try {
            GET_RIAL = MethodHandles.lookup().findGetter(SMethodHandle.class, "real", MethodHandle.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

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

    /** @serial */public static final int NULL_HANDLE                   = 0xFF;
    // direct
    /** @serial */public static final int DIRECT_METHOD_HANDLE          = 0x00;
    /** @serial */public static final int DIRECT_SPECIAL_METHOD_HANDLE  = 0x01;
    /** @serial */public static final int DIRECT_CONSTRUCTOR_HANDLE     = 0x02;
    /** @serial */public static final int DIRECT_FIELD_GETTER_HANDLE    = 0x03;
    /** @serial */public static final int DIRECT_FIELD_SETTER_HANDLE    = 0x04;
    // for HandleAndClassAndIntSMethodHandle
    /** @serial */public static final int COLLECTOR_HANDLE              = 0x05;
    /** @serial */public static final int VARARGS_COLLECTOR_HANDLE      = 0x06;
    /** @serial */public static final int SPREADER_HANDLE               = 0x07;
    /** @serial */public static final int FIXED_ARITY_HANDLE            = 0x08;
    /** @serial */public static final int ARRAY_GETTER_HANDLE           = 0x09;
    /** @serial */public static final int ARRAY_SETTER_HANDLE           = 0x0A;
    /** @serial */public static final int IDENTITY_HANDLE               = 0x0B;
    // for HandleAndTypeSMethodHandle
    /** @serial */public static final int AS_CAST_HANDLE                = 0x0C;
    /** @serial */public static final int EXPLICIT_CAST_HANDLE          = 0x0D;
    /** @serial */public static final int INVOKER_HANDLE                = 0x0E;
    /** @serial */public static final int EXACT_INVOKER_HANDLE          = 0x0F;
    /** @serial */public static final int SPREAD_INVOKER_HANDLE         = 0x10;
    // for HandlesSMethodHandle
    /** @serial */public static final int FILTER_ARGUMENTS_HANDLE       = 0x11;
    /** @serial */public static final int COLLECT_ARGUMENTS_HANDLE      = 0x12;
    /** @serial */public static final int FILTER_RETURN_HANDLE          = 0x13;
    /** @serial */public static final int FOLD_ARGUMENTS_HANDLE         = 0x14;
    /** @serial */public static final int GUARD_WITH_TEST_HANDLE        = 0x15;
    // others
    /** @serial */public static final int CONSTANT_HANDLE               = 0x16;
    /** @serial */public static final int INSERT_ARGUMENTS_HANDLE       = 0x17;
    /** @serial */public static final int CATCH_EXCEPTION_HANDLE        = 0x18;
    /** @serial */public static final int THROW_HANDLE                  = 0x19;
    /** @serial */public static final int PERMUTE_ARGUMENTS_HANDLE      = 0x1A;

    private final static class DirectMethodSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        boolean isSpecial;

        DirectMethodSMethodHandle(final MethodHandle real, boolean isSpecial) {
            super(real);
            // check is direct method method handle.
            Utils.doPrivileged(() -> { MethodHandles.reflectAs(Method.class, real); });
            this.isSpecial = isSpecial;
        }

        @Override
        protected int getTag() {
            return isSpecial ? DIRECT_SPECIAL_METHOD_HANDLE : DIRECT_METHOD_HANDLE;
        }

        private static DirectMethodSMethodHandle read(int tag, ObjectInput stream)
                throws IOException, ClassNotFoundException {
            Class<?> owner = (Class<?>)stream.readObject();
            Class<?> result = (Class<?>)stream.readObject();
            String name = stream.readUTF();
            int length = stream.readUnsignedByte();
            Class<?>[] parameters = new Class<?>[length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = (Class<?>)stream.readObject();
            }

            try {
                Method method = SMethodHandle.findMethod(owner, name, result, parameters);
                return new DirectMethodSMethodHandle(MethodHandles.publicLookup().unreflect(method),
                        tag == DIRECT_SPECIAL_METHOD_HANDLE);
            } catch (NoSuchMethodException e) {
                throw new IOException("can't found method: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void writeInternal(ObjectOutput stream) throws IOException {
            Method method = Utils.doPrivileged(() -> MethodHandles.reflectAs(Method.class, real));
            if (isSpecial && !Modifier.isPrivate(method.getModifiers())) {
                throw new NotSerializableException("special invocation to non-private method is not serializable.");
            }
            Class<?> owner = method.getDeclaringClass();
            String name = method.getName();
            Class<?>[] parameters = method.getParameterTypes();
            Class<?> result = method.getReturnType();

            stream.writeObject(owner);
            stream.writeObject(result);
            stream.writeUTF(name);
            stream.writeByte(parameters.length);
            for (Class<?> parameter : parameters) {
                stream.writeObject(parameter);
            }
        }
    }

    private final static class DirectConstructorSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        DirectConstructorSMethodHandle(final MethodHandle real) {
            super(real);
            // check is direct method method handle.
            Utils.doPrivileged(() -> { MethodHandles.reflectAs(Constructor.class, real); });
        }

        @Override
        int getTag() {
            return DIRECT_CONSTRUCTOR_HANDLE;
        }

        private static DirectConstructorSMethodHandle read(int tag, ObjectInput stream)
                throws IOException, ClassNotFoundException {
            Class<?> owner = (Class<?>)stream.readObject();
            int length = stream.readUnsignedByte();
            Class<?>[] parameters = new Class<?>[length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = (Class<?>)stream.readObject();
            }

            try {
                Constructor<?> constructor = SMethodHandle.findConstructor(owner, parameters);
                return new DirectConstructorSMethodHandle(MethodHandles.publicLookup().unreflectConstructor(constructor));
            } catch (NoSuchMethodException e) {
                throw new IOException("can't found method: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            Constructor<?> method = Utils.doPrivileged(() -> MethodHandles.reflectAs(Constructor.class, real));
            if (!Modifier.isPrivate(method.getModifiers())) {
                throw new NotSerializableException("special invocation to non-private method is not serializable.");
            }
            Class<?> owner = method.getDeclaringClass();
            Class<?>[] parameters = method.getParameterTypes();

            stream.writeObject(owner);
            stream.writeByte(parameters.length);
            for (Class<?> parameter : parameters) {
                stream.writeObject(parameter);
            }
        }
    }

    private final static class DirectFieldSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        DirectFieldSMethodHandle(final MethodHandle real) {
            super(real);
            // check is direct field method handle.
            Utils.doPrivileged(() -> { MethodHandles.reflectAs(Field.class, real); });
        }

        @Override
        int getTag() {
            // if setter, will return void.
            return real.type().returnType() == void.class
                    ? DIRECT_FIELD_SETTER_HANDLE
                    : DIRECT_FIELD_GETTER_HANDLE;
        }

        private static DirectFieldSMethodHandle read(int tag, ObjectInput stream)
                throws IOException, ClassNotFoundException {
            Class<?> owner = (Class<?>)stream.readObject();
            String name = stream.readUTF();
            Class<?> type = (Class<?>)stream.readObject();
            boolean isGet = tag == DIRECT_FIELD_GETTER_HANDLE;

            try {
                Field field = SMethodHandle.findField(owner, name, type);
                MethodHandle handle;
                if (isGet) handle = MethodHandles.publicLookup().unreflectGetter(field);
                else handle = MethodHandles.publicLookup().unreflectSetter(field);
                return new DirectFieldSMethodHandle(handle);
            } catch (NoSuchFieldException e) {
                throw new IOException("can't found field: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override void writeInternal(ObjectOutput stream) throws IOException {
            Field field = Utils.doPrivileged(() -> MethodHandles.reflectAs(Field.class, real));
            Class<?> owner = field.getDeclaringClass();
            String name = field.getName();
            Class<?> type = field.getType();

            stream.writeObject(owner);
            stream.writeUTF(name);
            stream.writeObject(type);
        }
    }

    final static class HandleAndClassAndIntSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle base;
        private final Class<?> arrayType;
        private final byte type;
        private final byte length;

        public HandleAndClassAndIntSMethodHandle(SMethodHandle base, Class<?> arrayType, int length, int type) {
            super(makeReal(base, arrayType, length, type));
            this.base = base;
            this.arrayType = arrayType;
            this.type = (byte) type;
            this.length = (byte)length;
        }

        private static MethodHandle makeReal(SMethodHandle base, Class<?> arrayType, int length, int type) {
            MethodHandle real;
            switch (type) {
                case COLLECTOR_HANDLE:
                    real = base.getReal().asCollector(arrayType, length);
                    break;
                case VARARGS_COLLECTOR_HANDLE:
                    real = base.getReal().asVarargsCollector(arrayType);
                    length = 0;
                    break;
                case SPREADER_HANDLE:
                    real = base.getReal().asSpreader(arrayType, length);
                    break;
                case FIXED_ARITY_HANDLE:
                    real = base.getReal().asFixedArity();
                    length = 0;
                    break;
                case ARRAY_GETTER_HANDLE:
                    real = MethodHandles.arrayElementGetter(arrayType);
                    length = 0;
                    break;
                case ARRAY_SETTER_HANDLE:
                    real = MethodHandles.arrayElementSetter(arrayType);
                    length = 0;
                    break;
                case IDENTITY_HANDLE:
                    real = MethodHandles.identity(arrayType);
                    length = 0;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            return real;
        }

        @Override
        int getTag() {
            return type & 0xFF;
        }

        private static HandleAndClassAndIntSMethodHandle read(int tag, ObjectInput stream)
                throws IOException, ClassNotFoundException {
            SMethodHandle base = SMethodHandle.readHandle(stream);
            Class<?> arrayType;
            int length;

            switch (tag) {
                case COLLECTOR_HANDLE:
                case SPREADER_HANDLE:
                    arrayType = (Class<?>) stream.readObject();
                    length = stream.readUnsignedByte();
                    break;
                case VARARGS_COLLECTOR_HANDLE:
                case ARRAY_SETTER_HANDLE:
                case ARRAY_GETTER_HANDLE:
                case IDENTITY_HANDLE:
                    arrayType = (Class<?>) stream.readObject();
                    length = 0;
                    break;
                case FIXED_ARITY_HANDLE:
                    arrayType = null;
                    length = 0;
                    break;
                default:
                    throw new AssertionError(String.format("invalid type: %08x", tag));
            }
            return new HandleAndClassAndIntSMethodHandle(base, arrayType, length, tag);
        }

        @Override void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, base);

            switch (getTag()) {
                case COLLECTOR_HANDLE:
                case SPREADER_HANDLE:
                    stream.writeObject(arrayType);
                    stream.writeByte(length);
                    break;
                case VARARGS_COLLECTOR_HANDLE:
                case ARRAY_SETTER_HANDLE:
                case ARRAY_GETTER_HANDLE:
                case IDENTITY_HANDLE:
                    stream.writeObject(arrayType);
                    break;
                case FIXED_ARITY_HANDLE:
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", getTag()));
            }
        }
    }

    final static class HandleAndTypeSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle base;
        private final MethodType methodType;
        private final byte type;
        private final byte value;

        public HandleAndTypeSMethodHandle(SMethodHandle base, MethodType methodType, int value, int type) {
            super(makeReal(base, methodType, value, type));
            this.base = base;
            this.methodType = methodType;
            this.type = (byte)type;
            this.value = (byte)value;
        }

        private static MethodHandle makeReal(SMethodHandle base, MethodType methodType, int value, int type) {
            MethodHandle real;
            switch (type) {
                case AS_CAST_HANDLE:
                    real = base.getReal().asType(methodType);
                    break;
                case EXPLICIT_CAST_HANDLE:
                    real = MethodHandles.explicitCastArguments(base.getReal(), methodType);
                    break;
                case INVOKER_HANDLE:
                    real = MethodHandles.invoker(methodType);
                    real = MethodHandles.filterArguments(real, 0, GET_RIAL);
                    break;
                case EXACT_INVOKER_HANDLE:
                    real = MethodHandles.exactInvoker(methodType);
                    real = MethodHandles.filterArguments(real, 0, GET_RIAL);
                    break;
                case SPREAD_INVOKER_HANDLE:
                    real = MethodHandles.spreadInvoker(methodType, value);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            return real;
        }

        @Override
        int getTag() {
            return type & 0xFF;
        }

        private static HandleAndTypeSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            final SMethodHandle base = SMethodHandle.readHandle(stream);
            final MethodType methodType = (MethodType) stream.readObject();
            final int value;
            switch (tag) {
                case AS_CAST_HANDLE:
                case EXPLICIT_CAST_HANDLE:
                case INVOKER_HANDLE:
                case EXACT_INVOKER_HANDLE:
                    value = 0;
                    break;
                case SPREAD_INVOKER_HANDLE:
                    value = stream.readUnsignedByte();
                    break;
                default:
                    throw new AssertionError(String.format("invalid type: %08x", tag));
            }
            return new HandleAndTypeSMethodHandle(base, methodType, value, tag);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, base);
            stream.writeObject(methodType);
            switch (getTag()) {
                case AS_CAST_HANDLE:
                case EXPLICIT_CAST_HANDLE:
                case INVOKER_HANDLE:
                case EXACT_INVOKER_HANDLE:
                    break;
                case SPREAD_INVOKER_HANDLE:
                    stream.writeByte(value);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", getTag()));
            }
        }
    }

    final static class HandlesSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle first;
        private final SMethodHandle[] handles;
        private final byte type;
        private final byte value;

        public HandlesSMethodHandle(int value, int type, SMethodHandle first, SMethodHandle... handles) {
            super(makeReal(first, handles, value, type));
            this.first = first;
            this.handles = handles.clone();
            this.type = (byte)type;
            this.value = (byte)value;
        }

        private static MethodHandle makeReal(SMethodHandle first, SMethodHandle[] handles, int value, int type) {
            MethodHandle real;
            switch (type) {
                case FILTER_ARGUMENTS_HANDLE:
                    MethodHandle[] reals;
                    reals = new MethodHandle[handles.length];
                    for (int i = 0; i < reals.length; i++) {
                        if (handles[i] != null)
                            reals[i] = handles[i].getReal();
                    }
                    real = MethodHandles.filterArguments(first.getReal(), value, reals);
                    break;
                case COLLECT_ARGUMENTS_HANDLE:
                    real = MethodHandles.collectArguments(first.getReal(), value, handles[0].getReal());
                    break;
                case FILTER_RETURN_HANDLE:
                    real = MethodHandles.filterReturnValue(first.getReal(), handles[0].getReal());
                    break;
                case FOLD_ARGUMENTS_HANDLE:
                    real = MethodHandles.foldArguments(first.getReal(), handles[0].getReal());
                    break;
                case GUARD_WITH_TEST_HANDLE:
                    real = MethodHandles.guardWithTest(first.getReal(), handles[0].getReal(), handles[1].getReal());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", type));
            }
            return real;
        }

        @Override
        int getTag() {
            return type & 0xFF;
        }

        private static HandlesSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            SMethodHandle first = SMethodHandle.readHandle(stream);
            SMethodHandle[] handles;
            int value;
            switch (tag) {
                case FILTER_ARGUMENTS_HANDLE:
                    value = stream.readUnsignedByte();
                    int length = stream.readUnsignedByte();
                    handles = new SMethodHandle[length];
                    for (int i = 0; i < handles.length; i++) {
                        handles[i] = SMethodHandle.readHandle(stream);
                    }
                    break;
                case COLLECT_ARGUMENTS_HANDLE:
                    value = stream.readUnsignedByte();
                    handles = new SMethodHandle[] {SMethodHandle.readHandle(stream)};
                    break;
                case FILTER_RETURN_HANDLE:
                case FOLD_ARGUMENTS_HANDLE:
                    value = 0;
                    handles = new SMethodHandle[] {SMethodHandle.readHandle(stream)};
                    break;
                case GUARD_WITH_TEST_HANDLE:
                    value = 0;
                    handles = new SMethodHandle[] {SMethodHandle.readHandle(stream), SMethodHandle.readHandle(stream)};
                    break;
                default:
                    throw new AssertionError(String.format("invalid type: %08x", tag));
            }
            return new HandlesSMethodHandle(value, tag, first, handles);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, first);
            switch (getTag()) {
                case FILTER_ARGUMENTS_HANDLE:
                    stream.writeByte(value);
                    stream.writeByte(handles.length);
                    for (int i = 0; i < handles.length; i++) {
                        SMethodHandle.writeHandle(stream, handles[i]);
                    }
                    break;
                case COLLECT_ARGUMENTS_HANDLE:
                    stream.writeByte(value);
                    SMethodHandle.writeHandle(stream, handles[0]);
                    break;
                case FILTER_RETURN_HANDLE:
                case FOLD_ARGUMENTS_HANDLE:
                    SMethodHandle.writeHandle(stream, handles[0]);
                    break;
                case GUARD_WITH_TEST_HANDLE:
                    SMethodHandle.writeHandle(stream, handles[0]);
                    SMethodHandle.writeHandle(stream, handles[1]);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid type: %08x", getTag()));
            }
        }
    }

    final static class ConstantSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final Class<?> type;
        private final Object value;

        public ConstantSMethodHandle(Class<?> type, Object value) {
            super(MethodHandles.constant(type, value));
            this.type = type;
            this.value = value;
        }

        @Override
        int getTag() {
            return CONSTANT_HANDLE;
        }

        private static ConstantSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            return new ConstantSMethodHandle((Class<?>) stream.readObject(), stream.readObject());
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            stream.writeObject(type);
            stream.writeObject(value);
        }
    }

    final static class InsertArgumentsSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle target;
        private final Object[] values;
        private final byte pos;

        public InsertArgumentsSMethodHandle(SMethodHandle target, int pos, Object[] values) {
            super(MethodHandles.insertArguments(target.getReal(), pos, values));
            this.target = target;
            this.pos = (byte) pos;
            this.values = values.clone();
        }

        @Override
        int getTag() {
            return INSERT_ARGUMENTS_HANDLE;
        }

        private static InsertArgumentsSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            final SMethodHandle target = SMethodHandle.readHandle(stream);
            final int pos = stream.readUnsignedByte();
            final Object[] values = new Object[stream.readUnsignedByte()];
            for (int i = 0; i < values.length; i++) {
                values[i] = stream.readObject();
            }

            return new InsertArgumentsSMethodHandle(target, pos, values);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, target);
            stream.writeByte(pos);
            stream.writeByte(values.length);
            for (Object value : values) {
                stream.writeObject(value);
            }
        }
    }

    final static class CatchExceptionSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle target;
        private final Class<? extends Throwable> exceptionType;
        private final SMethodHandle handle;

        public CatchExceptionSMethodHandle(SMethodHandle target, Class<? extends Throwable> exceptionType, SMethodHandle handle) {
            super(MethodHandles.catchException(target.real, exceptionType, handle.real));
            this.target = target;
            this.exceptionType = exceptionType;
            this.handle = handle;
        }

        @Override
        int getTag() {
            return CATCH_EXCEPTION_HANDLE;
        }

        private static CatchExceptionSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            final SMethodHandle target = SMethodHandle.readHandle(stream);
            final Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) stream.readObject();
            final SMethodHandle handle = SMethodHandle.readHandle(stream);
            return new CatchExceptionSMethodHandle(target, exceptionType, handle);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, target);
            stream.writeObject(exceptionType);
            SMethodHandle.writeHandle(stream, handle);
        }
    }

    final static class ThrowSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final Class<?> returnType;
        private final Class<? extends Throwable> exceptionType;

        public ThrowSMethodHandle(Class<?> returnType, Class<? extends Throwable> exceptionType) {
            super(MethodHandles.throwException(returnType, exceptionType));
            this.returnType = returnType;
            this.exceptionType = exceptionType;
        }

        @Override
        int getTag() {
            return THROW_HANDLE;
        }

        private static ThrowSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            final Class<?> returnType = (Class<?>) stream.readObject();
            final Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) stream.readObject();
            return new ThrowSMethodHandle(returnType, exceptionType);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            stream.writeObject(returnType);
            stream.writeObject(exceptionType);
        }
    }

    final static class PermuteArgumentsSMethodHandle extends SMethodHandle {
        private static final long serialVersionUID = 1;

        private final SMethodHandle target;
        private final MethodType newType;
        private final int[] reorder;

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

        @Override
        int getTag() {
            return PERMUTE_ARGUMENTS_HANDLE;
        }

        private static PermuteArgumentsSMethodHandle read(int tag, ObjectInput stream) throws IOException, ClassNotFoundException {
            final SMethodHandle target = SMethodHandle.readHandle(stream);
            final MethodType newType = (MethodType) stream.readObject();
            final int[] reorder = new int[stream.readUnsignedByte()];
            for (int i = 0; i < reorder.length; i++) {
                reorder[i] = stream.readUnsignedByte();
            }
            return new PermuteArgumentsSMethodHandle(target, newType, reorder);
        }

        @Override
        void writeInternal(ObjectOutput stream) throws IOException {
            SMethodHandle.writeHandle(stream, target);
            stream.writeObject(newType);
            stream.writeByte(reorder.length);
            for (int i = 0; i < reorder.length; i++) {
                stream.writeByte(reorder[i]);
            }
        }
    }

    /**
     * @return returns SerializationProxy instance.
     */
    protected Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        throw new InvalidObjectException("Proxy required");
    }

    private void readObject(ObjectInputStream stream) throws IOException {
        throw new InvalidObjectException("Proxy required");
    }

    private final static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1;

        private transient SMethodHandle handle;

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeByte(handle.getTag());
            handle.writeInternal(stream);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            handle = readHandle(stream);
            handle.getClass();
        }

        public SerializationProxy(SMethodHandle handle) {
            this.handle = handle;
        }

        private Object readResolve() {
            assert handle != null;
            return handle;
        }
    }

    private static SMethodHandle readHandle(ObjectInput stream) throws IOException, ClassNotFoundException {
        int tag = stream.readUnsignedByte();
        switch (tag) {
            case NULL_HANDLE:
                return null;
            // directs
            case DIRECT_METHOD_HANDLE:
            case DIRECT_SPECIAL_METHOD_HANDLE:
                return DirectMethodSMethodHandle.read(tag, stream);
            case DIRECT_CONSTRUCTOR_HANDLE:
                return DirectConstructorSMethodHandle.read(tag, stream);
            case DIRECT_FIELD_GETTER_HANDLE:
            case DIRECT_FIELD_SETTER_HANDLE:
                return DirectFieldSMethodHandle.read(tag, stream);
            // for HandleAndClassAndIntSMethodHandle
            case COLLECTOR_HANDLE:
            case VARARGS_COLLECTOR_HANDLE:
            case SPREADER_HANDLE:
            case FIXED_ARITY_HANDLE:
            case ARRAY_GETTER_HANDLE:
            case ARRAY_SETTER_HANDLE:
            case IDENTITY_HANDLE:
                return HandleAndClassAndIntSMethodHandle.read(tag, stream);
            // for HandleAndTypeSMethodHandle
            case AS_CAST_HANDLE:
            case EXPLICIT_CAST_HANDLE:
            case INVOKER_HANDLE:
            case EXACT_INVOKER_HANDLE:
            case SPREAD_INVOKER_HANDLE:
                return HandleAndTypeSMethodHandle.read(tag, stream);
            // for HandlesSMethodHandle
            case FILTER_ARGUMENTS_HANDLE:
            case COLLECT_ARGUMENTS_HANDLE:
            case FILTER_RETURN_HANDLE:
            case FOLD_ARGUMENTS_HANDLE:
            case GUARD_WITH_TEST_HANDLE:
                return HandlesSMethodHandle.read(tag, stream);
            // others
            case CONSTANT_HANDLE:
                return ConstantSMethodHandle.read(tag, stream);
            case INSERT_ARGUMENTS_HANDLE:
                return InsertArgumentsSMethodHandle.read(tag, stream);
            case CATCH_EXCEPTION_HANDLE:
                return CatchExceptionSMethodHandle.read(tag, stream);
            case THROW_HANDLE:
                return ThrowSMethodHandle.read(tag, stream);
            case PERMUTE_ARGUMENTS_HANDLE:
                return PermuteArgumentsSMethodHandle.read(tag, stream);
            default:
                throw new IllegalArgumentException(String.format("invalid type: %02x", tag));
        }
    }

    private static void writeHandle(ObjectOutput stream, SMethodHandle handle) throws IOException {
        if (handle == null) {
            stream.writeByte(NULL_HANDLE);
        } else {
            stream.writeByte(handle.getTag());
            handle.writeInternal(stream);
        }
    }

    // abstracts
    abstract int getTag();
    abstract void writeInternal(ObjectOutput stream) throws IOException;

    // utilities
    private static Method findMethod(Class<?> owner, String name, Class<?> result, Class<?>[] parameters) throws NoSuchMethodException {
        for (Method declaredMethod : owner.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(name)
                    && Arrays.equals(declaredMethod.getGenericParameterTypes(), parameters)
                    && declaredMethod.getReturnType() == result) {
                declaredMethod.setAccessible(true);
                return declaredMethod;
            }
        }
        throw new NoSuchMethodException(methodToString(owner, name, result, parameters));
    }

    private static Constructor<?> findConstructor(Class<?> owner, Class<?>[] parameters) throws NoSuchMethodException {
        for (Constructor<?> declaredConstructor : owner.getDeclaredConstructors()) {
            if (Arrays.equals(declaredConstructor.getGenericParameterTypes(), parameters)) {
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            }
        }
        throw new NoSuchMethodException(methodToString(owner, "<init>", void.class, parameters));
    }

    private static Field findField(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        for (Field declaredField : owner.getDeclaredFields()) {
            if (declaredField.getName().equals(name)
                    && declaredField.getType() == type)
                declaredField.setAccessible(true);
            return declaredField;
        }
        throw new NoSuchFieldException(fieldToString(owner, name, type));
    }

    public static String methodToString(Class<?> owner, String name, Class<?> result, Class<?>[] parameters) {
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

    public static String fieldToString(Class<?> owner, String name, Class<?> type) {
        return owner.getName() + '.' + name + '/' + type;
    }
    
    private static final long serialVersionUID = 1;
}
