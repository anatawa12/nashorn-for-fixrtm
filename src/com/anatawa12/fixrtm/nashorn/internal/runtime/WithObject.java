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

package com.anatawa12.fixrtm.nashorn.internal.runtime;

import static com.anatawa12.fixrtm.nashorn.internal.lookup.Lookup.MH;
import static com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle;
import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles;
import java.lang.invoke.MethodType;
import com.anatawa12.fixrtm.nashorn.invoke.SSwitchPoint;
import com.anatawa12.fixrtm.nashorn.dynalink.CallSiteDescriptor;
import com.anatawa12.fixrtm.nashorn.dynalink.linker.GuardedInvocation;
import com.anatawa12.fixrtm.nashorn.dynalink.linker.LinkRequest;
import com.anatawa12.fixrtm.nashorn.dynalink.support.CallSiteDescriptorFactory;
import com.anatawa12.fixrtm.nashorn.api.scripting.AbstractJSObject;
import com.anatawa12.fixrtm.nashorn.api.scripting.ScriptObjectMirror;
import com.anatawa12.fixrtm.nashorn.internal.runtime.linker.NashornCallSiteDescriptor;
import com.anatawa12.fixrtm.nashorn.internal.runtime.linker.NashornGuards;

/**
 * This class supports the handling of scope in a with body.
 *
 */
public final class WithObject extends Scope {
    private static final SMethodHandle WITHEXPRESSIONGUARD    = findOwnMH("withExpressionGuard",  boolean.class, Object.class, PropertyMap.class, SSwitchPoint[].class);
    private static final SMethodHandle WITHEXPRESSIONFILTER   = findOwnMH("withFilterExpression", Object.class, Object.class);
    private static final SMethodHandle WITHSCOPEFILTER        = findOwnMH("withFilterScope",      Object.class, Object.class);
    private static final SMethodHandle BIND_TO_EXPRESSION_OBJ = findOwnMH("bindToExpression",     Object.class, Object.class, Object.class);
    private static final SMethodHandle BIND_TO_EXPRESSION_FN  = findOwnMH("bindToExpression",     Object.class, ScriptFunction.class, Object.class);

    /** With expression object. */
    private final ScriptObject expression;

    /**
     * Constructor
     *
     * @param scope scope object
     * @param expression with expression
     */
    WithObject(final ScriptObject scope, final ScriptObject expression) {
        super(scope, null);
        this.expression = expression;
    }

    /**
     * Delete a property based on a key.
     * @param key Any valid JavaScript value.
     * @param strict strict mode execution.
     * @return True if deleted.
     */
    @Override
    public boolean delete(final Object key, final boolean strict) {
        final ScriptObject self = expression;
        final String propName = JSType.toString(key);

        final FindProperty find = self.findProperty(propName, true);

        if (find != null) {
            return self.delete(propName, strict);
        }

        return false;
    }


    @Override
    public GuardedInvocation lookup(final CallSiteDescriptor desc, final LinkRequest request) {
        if (request.isCallSiteUnstable()) {
            // Fall back to megamorphic invocation which performs a complete lookup each time without further relinking.
            return super.lookup(desc, request);
        }

        // With scopes can never be observed outside of Nashorn code, so all call sites that can address it will of
        // necessity have a Nashorn descriptor - it is safe to cast.
        final NashornCallSiteDescriptor ndesc = (NashornCallSiteDescriptor)desc;
        FindProperty find = null;
        GuardedInvocation link = null;
        ScriptObject self;

        final boolean isNamedOperation;
        final String name;
        if (desc.getNameTokenCount() > 2) {
            isNamedOperation = true;
            name = desc.getNameToken(CallSiteDescriptor.NAME_OPERAND);
        } else {
            isNamedOperation = false;
            name = null;
        }

        self = expression;
        if (isNamedOperation) {
             find = self.findProperty(name, true);
        }

        if (find != null) {
            link = self.lookup(desc, request);
            if (link != null) {
                return fixExpressionCallSite(ndesc, link);
            }
        }

        final ScriptObject scope = getProto();
        if (isNamedOperation) {
            find = scope.findProperty(name, true);
        }

        if (find != null) {
            return fixScopeCallSite(scope.lookup(desc, request), name, find.getOwner());
        }

        // the property is not found - now check for
        // __noSuchProperty__ and __noSuchMethod__ in expression
        if (self != null) {
            final String fallBack;

            final String operator = CallSiteDescriptorFactory.tokenizeOperators(desc).get(0);

            switch (operator) {
            case "callMethod":
                throw new AssertionError(); // Nashorn never emits callMethod
            case "getMethod":
                fallBack = NO_SUCH_METHOD_NAME;
                break;
            case "getProp":
            case "getElem":
                fallBack = NO_SUCH_PROPERTY_NAME;
                break;
            default:
                fallBack = null;
                break;
            }

            if (fallBack != null) {
                find = self.findProperty(fallBack, true);
                if (find != null) {
                    switch (operator) {
                    case "getMethod":
                        link = self.noSuchMethod(desc, request);
                        break;
                    case "getProp":
                    case "getElem":
                        link = self.noSuchProperty(desc, request);
                        break;
                    default:
                        break;
                    }
                }
            }

            if (link != null) {
                return fixExpressionCallSite(ndesc, link);
            }
        }

        // still not found, may be scope can handle with it's own
        // __noSuchProperty__, __noSuchMethod__ etc.
        link = scope.lookup(desc, request);

        if (link != null) {
            return fixScopeCallSite(link, name, null);
        }

        return null;
    }

    /**
     * Overridden to try to find the property first in the expression object (and its prototypes), and only then in this
     * object (and its prototypes).
     *
     * @param key  Property key.
     * @param deep Whether the search should look up proto chain.
     * @param start the object on which the lookup was originally initiated
     *
     * @return FindPropertyData or null if not found.
     */
    @Override
    protected FindProperty findProperty(final String key, final boolean deep, final ScriptObject start) {
        // We call findProperty on 'expression' with 'expression' itself as start parameter.
        // This way in ScriptObject.setObject we can tell the property is from a 'with' expression
        // (as opposed from another non-scope object in the proto chain such as Object.prototype).
        final FindProperty exprProperty = expression.findProperty(key, true, expression);
        if (exprProperty != null) {
             return exprProperty;
        }
        return super.findProperty(key, deep, start);
    }

    @Override
    protected Object invokeNoSuchProperty(final String name, final boolean isScope, final int programPoint) {
        FindProperty find = expression.findProperty(NO_SUCH_PROPERTY_NAME, true);
        if (find != null) {
            final Object func = find.getObjectValue();
            if (func instanceof ScriptFunction) {
                final ScriptFunction sfunc = (ScriptFunction)func;
                final Object self = isScope && sfunc.isStrict()? UNDEFINED : expression;
                return ScriptRuntime.apply(sfunc, self, name);
            }
        }

        return getProto().invokeNoSuchProperty(name, isScope, programPoint);
    }

    @Override
    public void setSplitState(final int state) {
        ((Scope) getNonWithParent()).setSplitState(state);
    }

    @Override
    public int getSplitState() {
        return ((Scope) getNonWithParent()).getSplitState();
    }

    @Override
    public void addBoundProperties(final ScriptObject source, final Property[] properties) {
        // Declared variables in nested eval go to first normal (non-with) parent scope.
        getNonWithParent().addBoundProperties(source, properties);
    }

    /**
     * Get first parent scope that is not an instance of WithObject.
     */
    private ScriptObject getNonWithParent() {
        ScriptObject proto = getProto();

        while (proto != null && proto instanceof WithObject) {
            proto = proto.getProto();
        }

        return proto;
    }

    private static GuardedInvocation fixReceiverType(final GuardedInvocation link, final SMethodHandle filter) {
        // The receiver may be an Object or a ScriptObject.
        final MethodType invType = link.getInvocation().type();
        final MethodType newInvType = invType.changeParameterType(0, filter.type().returnType());
        return link.asType(newInvType);
    }

    private static GuardedInvocation fixExpressionCallSite(final NashornCallSiteDescriptor desc, final GuardedInvocation link) {
        // If it's not a getMethod, just add an expression filter that converts WithObject in "this" position to its
        // expression.
        if (!"getMethod".equals(desc.getFirstOperator())) {
            return fixReceiverType(link, WITHEXPRESSIONFILTER).filterArguments(0, WITHEXPRESSIONFILTER);
        }

        final SMethodHandle linkInvocation      = link.getInvocation();
        final MethodType   linkType            = linkInvocation.type();
        final boolean      linkReturnsFunction = ScriptFunction.class.isAssignableFrom(linkType.returnType());

        return link.replaceMethods(
                // Make sure getMethod will bind the script functions it receives to WithObject.expression
                MH.foldArguments(
                        linkReturnsFunction ?
                                BIND_TO_EXPRESSION_FN :
                                BIND_TO_EXPRESSION_OBJ,
                        filterReceiver(
                                linkInvocation.asType(
                                        linkType.changeReturnType(
                                                linkReturnsFunction ?
                                                        ScriptFunction.class :
                                                        Object.class).
                                                            changeParameterType(
                                                                    0,
                                                                    Object.class)),
                                        WITHEXPRESSIONFILTER)),
                         filterGuardReceiver(link, WITHEXPRESSIONFILTER));
     // No clever things for the guard -- it is still identically filtered.

    }

    private GuardedInvocation fixScopeCallSite(final GuardedInvocation link, final String name, final ScriptObject owner) {
        final GuardedInvocation newLink             = fixReceiverType(link, WITHSCOPEFILTER);
        final SMethodHandle      expressionGuard     = expressionGuard(name, owner);
        final SMethodHandle      filterGuardReceiver = filterGuardReceiver(newLink, WITHSCOPEFILTER);
        return link.replaceMethods(
                filterReceiver(
                        newLink.getInvocation(),
                        WITHSCOPEFILTER),
                NashornGuards.combineGuards(
                        expressionGuard,
                        filterGuardReceiver));
    }

    private static SMethodHandle filterGuardReceiver(final GuardedInvocation link, final SMethodHandle receiverFilter) {
        final SMethodHandle test = link.getGuard();
        if (test == null) {
            return null;
        }

        final Class<?> receiverType = test.type().parameterType(0);
        final SMethodHandle filter = MH.asType(receiverFilter,
                receiverFilter.type().changeParameterType(0, receiverType).
                changeReturnType(receiverType));

        return filterReceiver(test, filter);
    }

    private static SMethodHandle filterReceiver(final SMethodHandle mh, final SMethodHandle receiverFilter) {
        //With expression filter == receiverFilter, i.e. receiver is cast to withobject and its expression returned
        return MH.filterArguments(mh, 0, receiverFilter.asType(receiverFilter.type().changeReturnType(mh.type().parameterType(0))));
    }

    /**
     * Drops the WithObject wrapper from the expression.
     * @param receiver WithObject wrapper.
     * @return The with expression.
     */
    public static Object withFilterExpression(final Object receiver) {
        return ((WithObject)receiver).expression;
    }

    @SuppressWarnings("unused")
    private static Object bindToExpression(final Object fn, final Object receiver) {
        if (fn instanceof ScriptFunction) {
            return bindToExpression((ScriptFunction) fn, receiver);
        } else if (fn instanceof ScriptObjectMirror) {
            final ScriptObjectMirror mirror = (ScriptObjectMirror)fn;
            if (mirror.isFunction()) {
                // We need to make sure correct 'this' is used for calls with Ident call
                // expressions. We do so here using an AbstractJSObject instance.
                return new AbstractJSObject() {
                    @Override
                    public Object call(final Object thiz, final Object... args) {
                        return mirror.call(withFilterExpression(receiver), args);
                    }
                };
            }
        }

        return fn;
    }

    private static Object bindToExpression(final ScriptFunction fn, final Object receiver) {
        return fn.createBound(withFilterExpression(receiver), ScriptRuntime.EMPTY_ARRAY);
    }

    private SMethodHandle expressionGuard(final String name, final ScriptObject owner) {
        final PropertyMap map = expression.getMap();
        final SSwitchPoint[] sp = expression.getProtoSwitchPoints(name, owner);
        return MH.insertArguments(WITHEXPRESSIONGUARD, 1, map, sp);
    }

    @SuppressWarnings("unused")
    private static boolean withExpressionGuard(final Object receiver, final PropertyMap map, final SSwitchPoint[] sp) {
        return ((WithObject)receiver).expression.getMap() == map && !hasBeenInvalidated(sp);
    }

    private static boolean hasBeenInvalidated(final SSwitchPoint[] switchPoints) {
        if (switchPoints != null) {
            for (final SSwitchPoint switchPoint : switchPoints) {
                if (switchPoint.hasBeenInvalidated()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Drops the WithObject wrapper from the scope.
     * @param receiver WithObject wrapper.
     * @return The with scope.
     */
    public static Object withFilterScope(final Object receiver) {
        return ((WithObject)receiver).getProto();
    }

    /**
     * Get the with expression for this {@code WithObject}
     * @return the with expression
     */
    public ScriptObject getExpression() {
        return expression;
    }

    private static SMethodHandle findOwnMH(final String name, final Class<?> rtype, final Class<?>... types) {
        return MH.findStatic(MethodHandles.lookup(), WithObject.class, name, MH.type(rtype, types));
    }
}
