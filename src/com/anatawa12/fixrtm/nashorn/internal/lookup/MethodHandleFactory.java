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

import static com.anatawa12.fixrtm.nashorn.internal.runtime.JSType.isString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle;
import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles;
import java.lang.invoke.MethodType;
import com.anatawa12.fixrtm.nashorn.invoke.SSwitchPoint;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Context;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Debug;
import com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptObject;
import com.anatawa12.fixrtm.nashorn.internal.runtime.logging.DebugLogger;
import com.anatawa12.fixrtm.nashorn.internal.runtime.logging.Loggable;
import com.anatawa12.fixrtm.nashorn.internal.runtime.logging.Logger;
import com.anatawa12.fixrtm.nashorn.internal.runtime.options.Options;

/**
 * This class is abstraction for all method handle, switchpoint and method type
 * operations. This enables the functionality interface to be subclassed and
 * instrumented, as it has been proven vital to keep the number of method
 * handles in the system down.
 *
 * All operations of the above type should go through this class, and not
 * directly into java.lang.invoke
 *
 */
public final class MethodHandleFactory {

    private static final SMethodHandles.Lookup PUBLIC_LOOKUP = SMethodHandles.publicLookup();
    private static final SMethodHandles.Lookup LOOKUP        = SMethodHandles.l(MethodHandles.lookup());

    private static final Level TRACE_LEVEL = Level.INFO;

    private MethodHandleFactory() {
    }

    /**
     * Runtime exception that collects every reason that a method handle lookup operation can go wrong
     */
    @SuppressWarnings("serial")
    public static class LookupException extends RuntimeException {
        /**
         * Constructor
         * @param e causing exception
         */
        public LookupException(final Exception e) {
            super(e);
        }
    }

    /**
     * Helper function that takes a class or an object with a toString override
     * and shortens it to notation after last dot. This is used to facilitiate
     * pretty printouts in various debug loggers - internal only
     *
     * @param obj class or object
     *
     * @return pretty version of object as string
     */
    public static String stripName(final Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof Class) {
            return ((Class<?>)obj).getSimpleName();
        }
        return obj.toString();
    }

    private static final MethodHandleFunctionality FUNC = new StandardMethodHandleFunctionality();
    private static final boolean PRINT_STACKTRACE = Options.getBooleanProperty("nashorn.methodhandles.debug.stacktrace");

    /**
     * Return the method handle functionality used for all method handle operations
     * @return a method handle functionality implementation
     */
    public static MethodHandleFunctionality getFunctionality() {
        return FUNC;
    }

    private static final SMethodHandle TRACE             = FUNC.findStatic(LOOKUP, MethodHandleFactory.class, "traceArgs",   MethodType.methodType(void.class, DebugLogger.class, String.class, int.class, Object[].class));
    private static final SMethodHandle TRACE_RETURN      = FUNC.findStatic(LOOKUP, MethodHandleFactory.class, "traceReturn", MethodType.methodType(Object.class, DebugLogger.class, Object.class));
    private static final SMethodHandle TRACE_RETURN_VOID = FUNC.findStatic(LOOKUP, MethodHandleFactory.class, "traceReturnVoid", MethodType.methodType(void.class, DebugLogger.class));

    private static final String VOID_TAG = "[VOID]";

    private static void err(final String str) {
        Context.getContext().getErr().println(str);
    }

    /**
     * Tracer that is applied before a value is returned from the traced function. It will output the return
     * value and its class
     *
     * @param value return value for filter
     * @return return value unmodified
     */
    static Object traceReturn(final DebugLogger logger, final Object value) {
        final String str = "    return" +
                (VOID_TAG.equals(value) ?
                        ";" :
                            " " + stripName(value) + "; // [type=" + (value == null ? "null]" : stripName(value.getClass()) + ']'));
        if (logger == null) {
            err(str);
        } else if (logger.isEnabled()) {
            logger.log(TRACE_LEVEL, str);
        }

        return value;
    }

    static void traceReturnVoid(final DebugLogger logger) {
        traceReturn(logger, VOID_TAG);
    }

    /**
     * Tracer that is applied before a function is called, printing the arguments
     *
     * @param tag  tag to start the debug printout string
     * @param paramStart param index to start outputting from
     * @param args arguments to the function
     */
    static void traceArgs(final DebugLogger logger, final String tag, final int paramStart, final Object... args) {
        final StringBuilder sb = new StringBuilder();

        sb.append(tag);

        for (int i = paramStart; i < args.length; i++) {
            if (i == paramStart) {
                sb.append(" => args: ");
            }

            sb.append('\'').
            append(stripName(argString(args[i]))).
            append('\'').
            append(' ').
            append('[').
            append("type=").
            append(args[i] == null ? "null" : stripName(args[i].getClass())).
            append(']');

            if (i + 1 < args.length) {
                sb.append(", ");
            }
        }

        if (logger == null) {
            err(sb.toString());
        } else {
            logger.log(TRACE_LEVEL, sb);
        }
        stacktrace(logger);
    }

    private static void stacktrace(final DebugLogger logger) {
        if (!PRINT_STACKTRACE) {
            return;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(baos);
        new Throwable().printStackTrace(ps);
        final String st = baos.toString();
        if (logger == null) {
            err(st);
        } else {
            logger.log(TRACE_LEVEL, st);
        }
    }

    private static String argString(final Object arg) {
        if (arg == null) {
            return "null";
        }

        if (arg.getClass().isArray()) {
            final List<Object> list = new ArrayList<>();
            for (final Object elem : (Object[])arg) {
                list.add('\'' + argString(elem) + '\'');
            }

            return list.toString();
        }

        if (arg instanceof ScriptObject) {
            return arg.toString() +
                    " (map=" + Debug.id(((ScriptObject)arg).getMap()) +
                    ')';
        }

        return arg.toString();
    }

    /**
     * Add a debug printout to a method handle, tracing parameters and return values
     * Output will be unconditional to stderr
     *
     * @param mh  method handle to trace
     * @param tag start of trace message
     * @return traced method handle
     */
    public static SMethodHandle addDebugPrintout(final SMethodHandle mh, final Object tag) {
        return addDebugPrintout(null, Level.OFF, mh, 0, true, tag);
    }

    /**
     * Add a debug printout to a method handle, tracing parameters and return values
     *
     * @param logger a specific logger to which to write the output
     * @param level level over which to print
     * @param mh  method handle to trace
     * @param tag start of trace message
     * @return traced method handle
     */
    public static SMethodHandle addDebugPrintout(final DebugLogger logger, final Level level, final SMethodHandle mh, final Object tag) {
        return addDebugPrintout(logger, level, mh, 0, true, tag);
    }

    /**
     * Add a debug printout to a method handle, tracing parameters and return values
     * Output will be unconditional to stderr
     *
     * @param mh  method handle to trace
     * @param paramStart first param to print/trace
     * @param printReturnValue should we print/trace return value if available?
     * @param tag start of trace message
     * @return  traced method handle
     */
    public static SMethodHandle addDebugPrintout(final SMethodHandle mh, final int paramStart, final boolean printReturnValue, final Object tag) {
        return addDebugPrintout(null, Level.OFF, mh, paramStart, printReturnValue, tag);
    }

    /**
     * Add a debug printout to a method handle, tracing parameters and return values
     *
     * @param logger a specific logger to which to write the output
     * @param level level over which to print
     * @param mh  method handle to trace
     * @param paramStart first param to print/trace
     * @param printReturnValue should we print/trace return value if available?
     * @param tag start of trace message
     * @return  traced method handle
     */
    public static SMethodHandle addDebugPrintout(final DebugLogger logger, final Level level, final SMethodHandle mh, final int paramStart, final boolean printReturnValue, final Object tag) {
        final MethodType type = mh.type();

        //if there is no logger, or if it's set to log only coarser events
        //than the trace level, skip and return
        if (logger == null || !logger.isLoggable(level)) {
            return mh;
        }

        assert TRACE != null;

        SMethodHandle trace = SMethodHandles.insertArguments(TRACE, 0, logger, tag, paramStart);

        trace = SMethodHandles.foldArguments(
                mh,
                trace.asCollector(
                        Object[].class,
                        type.parameterCount()).
                        asType(type.changeReturnType(void.class)));

        final Class<?> retType = type.returnType();
        if (printReturnValue) {
            if (retType != void.class) {
                final SMethodHandle traceReturn = SMethodHandles.insertArguments(TRACE_RETURN, 0, logger);
                trace = SMethodHandles.filterReturnValue(trace,
                        traceReturn.asType(
                                traceReturn.type().changeParameterType(0, retType).changeReturnType(retType)));
            } else {
                trace = SMethodHandles.filterReturnValue(trace, SMethodHandles.insertArguments(TRACE_RETURN_VOID, 0, logger));
            }
        }

        return trace;
    }

    /**
     * Class that marshalls all method handle operations to the java.lang.invoke
     * package. This exists only so that it can be subclassed and method handles created from
     * Nashorn made possible to instrument.
     *
     * All Nashorn classes should use the MethodHandleFactory for their method handle operations
     */
    @Logger(name="methodhandles")
    private static class StandardMethodHandleFunctionality implements MethodHandleFunctionality, Loggable {

        // for bootstrapping reasons, because a lot of static fields use MH for lookups, we
        // need to set the logger when the Global object is finished. This means that we don't
        // get instrumentation for public static final SMethodHandle SOMETHING = MH... in the builtin
        // classes, but that doesn't matter, because this is usually not where we want it
        private DebugLogger log = DebugLogger.DISABLED_LOGGER;

        public StandardMethodHandleFunctionality() {
        }

        @Override
        public DebugLogger initLogger(final Context context) {
            return this.log = context.getLogger(this.getClass());
        }

        @Override
        public DebugLogger getLogger() {
            return log;
        }

        protected static String describe(final Object... data) {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < data.length; i++) {
                final Object d = data[i];
                if (d == null) {
                    sb.append("<null> ");
                } else if (isString(d)) {
                    sb.append(d.toString());
                    sb.append(' ');
                } else if (d.getClass().isArray()) {
                    sb.append("[ ");
                    for (final Object da : (Object[])d) {
                        sb.append(describe(new Object[]{ da })).append(' ');
                    }
                    sb.append("] ");
                } else {
                    sb.append(d)
                    .append('{')
                    .append(Integer.toHexString(System.identityHashCode(d)))
                    .append('}');
                }

                if (i + 1 < data.length) {
                    sb.append(", ");
                }
            }

            return sb.toString();
        }

        public SMethodHandle debug(final SMethodHandle master, final String str, final Object... args) {
            if (log.isEnabled()) {
                if (PRINT_STACKTRACE) {
                    stacktrace(log);
                }
                return addDebugPrintout(log, Level.INFO, master, Integer.MAX_VALUE, false, str + ' ' + describe(args));
            }
            return master;
        }

        @Override
        public SMethodHandle filterArguments(final SMethodHandle target, final int pos, final SMethodHandle... filters) {
            final SMethodHandle mh = SMethodHandles.filterArguments(target, pos, filters);
            return debug(mh, "filterArguments", target, pos, filters);
        }

        @Override
        public SMethodHandle filterReturnValue(final SMethodHandle target, final SMethodHandle filter) {
            final SMethodHandle mh = SMethodHandles.filterReturnValue(target, filter);
            return debug(mh, "filterReturnValue", target, filter);
        }

        @Override
        public SMethodHandle guardWithTest(final SMethodHandle test, final SMethodHandle target, final SMethodHandle fallback) {
            final SMethodHandle mh = SMethodHandles.guardWithTest(test, target, fallback);
            return debug(mh, "guardWithTest", test, target, fallback);
        }

        @Override
        public SMethodHandle insertArguments(final SMethodHandle target, final int pos, final Object... values) {
            final SMethodHandle mh = SMethodHandles.insertArguments(target, pos, values);
            return debug(mh, "insertArguments", target, pos, values);
        }

        @Override
        public SMethodHandle dropArguments(final SMethodHandle target, final int pos, final Class<?>... values) {
            final SMethodHandle mh = SMethodHandles.dropArguments(target, pos, values);
            return debug(mh, "dropArguments", target, pos, values);
        }

        @Override
        public SMethodHandle dropArguments(final SMethodHandle target, final int pos, final List<Class<?>> values) {
            final SMethodHandle mh = SMethodHandles.dropArguments(target, pos, values);
            return debug(mh, "dropArguments", target, pos, values);
        }

        @Override
        public SMethodHandle asType(final SMethodHandle handle, final MethodType type) {
            final SMethodHandle mh = handle.asType(type);
            return debug(mh, "asType", handle, type);
        }

        @Override
        public SMethodHandle bindTo(final SMethodHandle handle, final Object x) {
            final SMethodHandle mh = handle.bindTo(x);
            return debug(mh, "bindTo", handle, x);
        }

        @Override
        public SMethodHandle foldArguments(final SMethodHandle target, final SMethodHandle combiner) {
            final SMethodHandle mh = SMethodHandles.foldArguments(target, combiner);
            return debug(mh, "foldArguments", target, combiner);
        }

        @Override
        public SMethodHandle explicitCastArguments(final SMethodHandle target, final MethodType type) {
            final SMethodHandle mh = SMethodHandles.explicitCastArguments(target, type);
            return debug(mh, "explicitCastArguments", target, type);
        }

        @Override
        public SMethodHandle arrayElementGetter(final Class<?> type) {
            final SMethodHandle mh = SMethodHandles.arrayElementGetter(type);
            return debug(mh, "arrayElementGetter", type);
        }

        @Override
        public SMethodHandle arrayElementSetter(final Class<?> type) {
            final SMethodHandle mh = SMethodHandles.arrayElementSetter(type);
            return debug(mh, "arrayElementSetter", type);
        }

        @Override
        public SMethodHandle throwException(final Class<?> returnType, final Class<? extends Throwable> exType) {
            final SMethodHandle mh = SMethodHandles.throwException(returnType, exType);
            return debug(mh, "throwException", returnType, exType);
        }

        @Override
        public SMethodHandle catchException(final SMethodHandle target, final Class<? extends Throwable> exType, final SMethodHandle handler) {
            final SMethodHandle mh = SMethodHandles.catchException(target, exType, handler);
            return debug(mh, "catchException", exType);
        }

        @Override
        public SMethodHandle constant(final Class<?> type, final Object value) {
            final SMethodHandle mh = SMethodHandles.constant(type, value);
            return debug(mh, "constant", type, value);
        }

        @Override
        public SMethodHandle identity(final Class<?> type) {
            final SMethodHandle mh = SMethodHandles.identity(type);
            return debug(mh, "identity", type);
        }

        @Override
        public SMethodHandle asCollector(final SMethodHandle handle, final Class<?> arrayType, final int arrayLength) {
            final SMethodHandle mh = handle.asCollector(arrayType, arrayLength);
            return debug(mh, "asCollector", handle, arrayType, arrayLength);
        }

        @Override
        public SMethodHandle asSpreader(final SMethodHandle handle, final Class<?> arrayType, final int arrayLength) {
            final SMethodHandle mh = handle.asSpreader(arrayType, arrayLength);
            return debug(mh, "asSpreader", handle, arrayType, arrayLength);
        }

        @Override
        public SMethodHandle getter(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final Class<?> type) {
            try {
                final SMethodHandle mh = explicitLookup.findGetter(clazz, name, type);
                return debug(mh, "getter", explicitLookup, clazz, name, type);
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle staticGetter(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final Class<?> type) {
            try {
                final SMethodHandle mh = explicitLookup.findStaticGetter(clazz, name, type);
                return debug(mh, "static getter", explicitLookup, clazz, name, type);
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle setter(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final Class<?> type) {
            try {
                final SMethodHandle mh = explicitLookup.findSetter(clazz, name, type);
                return debug(mh, "setter", explicitLookup, clazz, name, type);
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle staticSetter(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final Class<?> type) {
            try {
                final SMethodHandle mh = explicitLookup.findStaticSetter(clazz, name, type);
                return debug(mh, "static setter", explicitLookup, clazz, name, type);
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle find(final Method method) {
            try {
                final SMethodHandle mh = PUBLIC_LOOKUP.unreflect(method);
                return debug(mh, "find", method);
            } catch (final IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle findStatic(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final MethodType type) {
            try {
                final SMethodHandle mh = explicitLookup.findStatic(clazz, name, type);
                return debug(mh, "findStatic", explicitLookup, clazz, name, type);
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle findSpecial(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final MethodType type, final Class<?> thisClass) {
            try {
                final SMethodHandle mh = explicitLookup.findSpecial(clazz, name, type, thisClass);
                return debug(mh, "findSpecial", explicitLookup, clazz, name, type);
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SMethodHandle findVirtual(final SMethodHandles.Lookup explicitLookup, final Class<?> clazz, final String name, final MethodType type) {
            try {
                final SMethodHandle mh = explicitLookup.findVirtual(clazz, name, type);
                return debug(mh, "findVirtual", explicitLookup, clazz, name, type);
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new LookupException(e);
            }
        }

        @Override
        public SSwitchPoint createSwitchPoint() {
            final SSwitchPoint sp = new SSwitchPoint();
            log.log(TRACE_LEVEL, "createSwitchPoint ", sp);
            return sp;
        }

        @Override
        public SMethodHandle guardWithTest(final SSwitchPoint sp, final SMethodHandle before, final SMethodHandle after) {
            final SMethodHandle mh = sp.guardWithTest(before, after);
            return debug(mh, "guardWithTest", sp, before, after);
        }

        @Override
        public MethodType type(final Class<?> returnType, final Class<?>... paramTypes) {
            final MethodType mt = MethodType.methodType(returnType, paramTypes);
            log.log(TRACE_LEVEL, "methodType ", returnType, " ", Arrays.toString(paramTypes), " ", mt);
            return mt;
        }
    }
}
