/*
 * Copyright (c) 2010-2014, Oracle and/or its affiliates. All rights reserved.
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

import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandle;
import com.anatawa12.fixrtm.nashorn.invoke.SMethodHandles;
import java.lang.invoke.MethodHandles;

/**
 * Spill property
 */
public class SpillProperty extends AccessorProperty {
    private static final long serialVersionUID = 3028496245198669460L;

    private static final SMethodHandles.Lookup LOOKUP = SMethodHandles.l(MethodHandles.lookup());

    private static final SMethodHandle PARRAY_GETTER = MH.asType(MH.getter(LOOKUP, ScriptObject.class, "primitiveSpill",  long[].class), MH.type(long[].class, Object.class));
    private static final SMethodHandle OARRAY_GETTER = MH.asType(MH.getter(LOOKUP, ScriptObject.class, "objectSpill",  Object[].class), MH.type(Object[].class, Object.class));

    private static final SMethodHandle OBJECT_GETTER    = MH.filterArguments(MH.arrayElementGetter(Object[].class), 0, OARRAY_GETTER);
    private static final SMethodHandle PRIMITIVE_GETTER = MH.filterArguments(MH.arrayElementGetter(long[].class), 0, PARRAY_GETTER);
    private static final SMethodHandle OBJECT_SETTER    = MH.filterArguments(MH.arrayElementSetter(Object[].class), 0, OARRAY_GETTER);
    private static final SMethodHandle PRIMITIVE_SETTER = MH.filterArguments(MH.arrayElementSetter(long[].class), 0, PARRAY_GETTER);

    private static class Accessors {
        private SMethodHandle objectGetter;
        private SMethodHandle objectSetter;
        private SMethodHandle primitiveGetter;
        private SMethodHandle primitiveSetter;

        private final int slot;
        private final SMethodHandle ensureSpillSize;

        private static Accessors ACCESSOR_CACHE[] = new Accessors[512];

        //private static final Map<Integer, Reference<Accessors>> ACCESSOR_CACHE = Collections.synchronizedMap(new WeakHashMap<Integer, Reference<Accessors>>());

        Accessors(final int slot) {
            assert slot >= 0;
            this.slot = slot;
            this.ensureSpillSize = MH.asType(MH.insertArguments(ScriptObject.ENSURE_SPILL_SIZE, 1, slot), MH.type(Object.class, Object.class));
        }

        private static void ensure(final int slot) {
            int len = ACCESSOR_CACHE.length;
            if (slot >= len) {
                do {
                    len *= 2;
                } while (slot >= len);
                final Accessors newCache[] = new Accessors[len];
                System.arraycopy(ACCESSOR_CACHE, 0, newCache, 0, ACCESSOR_CACHE.length);
                ACCESSOR_CACHE = newCache;
            }
        }

        static SMethodHandle getCached(final int slot, final boolean isPrimitive, final boolean isGetter) {
            //Reference<Accessors> ref = ACCESSOR_CACHE.get(slot);
            ensure(slot);
            Accessors acc = ACCESSOR_CACHE[slot];
            if (acc == null) {
                acc = new Accessors(slot);
                ACCESSOR_CACHE[slot] = acc;
            }

            return acc.getOrCreate(isPrimitive, isGetter);
        }

        private static SMethodHandle primordial(final boolean isPrimitive, final boolean isGetter) {
            if (isPrimitive) {
                return isGetter ? PRIMITIVE_GETTER : PRIMITIVE_SETTER;
            }
            return isGetter ? OBJECT_GETTER : OBJECT_SETTER;
        }

        SMethodHandle getOrCreate(final boolean isPrimitive, final boolean isGetter) {
            SMethodHandle accessor;

            accessor = getInner(isPrimitive, isGetter);
            if (accessor != null) {
                return accessor;
            }

            accessor = primordial(isPrimitive, isGetter);
            accessor = MH.insertArguments(accessor, 1, slot);
            if (!isGetter) {
                accessor = MH.filterArguments(accessor, 0, ensureSpillSize);
            }
            setInner(isPrimitive, isGetter, accessor);

            return accessor;
        }

        void setInner(final boolean isPrimitive, final boolean isGetter, final SMethodHandle mh) {
            if (isPrimitive) {
                if (isGetter) {
                    primitiveGetter = mh;
                } else {
                    primitiveSetter = mh;
                }
            } else {
                if (isGetter) {
                    objectGetter = mh;
                } else {
                    objectSetter = mh;
                }
            }
        }

        SMethodHandle getInner(final boolean isPrimitive, final boolean isGetter) {
            if (isPrimitive) {
                return isGetter ? primitiveGetter : primitiveSetter;
            }
            return isGetter ? objectGetter : objectSetter;
        }
    }

    private static SMethodHandle primitiveGetter(final int slot, final int flags) {
        return (flags & DUAL_FIELDS) == DUAL_FIELDS ? Accessors.getCached(slot, true, true) : null;
    }
    private static SMethodHandle primitiveSetter(final int slot, final int flags) {
        return (flags & DUAL_FIELDS) == DUAL_FIELDS ? Accessors.getCached(slot, true, false) : null;
    }
    private static SMethodHandle objectGetter(final int slot) {
        return Accessors.getCached(slot, false, true);
    }
    private static SMethodHandle objectSetter(final int slot) {
        return Accessors.getCached(slot, false, false);
    }

    /**
     * Constructor for spill properties. Array getters and setters will be created on demand.
     *
     * @param key    the property key
     * @param flags  the property flags
     * @param slot   spill slot
     */
    public SpillProperty(final String key, final int flags, final int slot) {
        super(key, flags, slot, primitiveGetter(slot, flags), primitiveSetter(slot, flags), objectGetter(slot), objectSetter(slot));
    }

    /**
     * Constructor for spill properties with an initial type.
     * @param key         the property key
     * @param flags       the property flags
     * @param slot        spill slot
     * @param initialType initial type
     */
    public SpillProperty(final String key, final int flags, final int slot, final Class<?> initialType) {
        this(key, flags, slot);
        setType(hasDualFields() ? initialType : Object.class);
    }

    SpillProperty(final String key, final int flags, final int slot, final ScriptObject owner, final Object initialValue) {
        this(key, flags, slot);
        setInitialValue(owner, initialValue);
    }

    /**
     * Copy constructor
     * @param property other property
     */
    protected SpillProperty(final SpillProperty property) {
        super(property);
    }

    /**
     * Copy constructor
     * @param newType new type
     * @param property other property
     */
    protected SpillProperty(final SpillProperty property, final Class<?> newType) {
        super(property, newType);
    }

    @Override
    public Property copy() {
        return new SpillProperty(this);
    }

    @Override
    public Property copy(final Class<?> newType) {
        return new SpillProperty(this, newType);
    }

    @Override
    public boolean isSpill() {
        return true;
    }

    @Override
    void initMethodHandles(final Class<?> structure) {
        final int slot  = getSlot();
        primitiveGetter = primitiveGetter(slot, getFlags());
        primitiveSetter = primitiveSetter(slot, getFlags());
        objectGetter    = objectGetter(slot);
        objectSetter    = objectSetter(slot);
    }
}
