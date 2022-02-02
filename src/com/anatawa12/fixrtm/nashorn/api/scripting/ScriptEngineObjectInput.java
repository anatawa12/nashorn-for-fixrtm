package com.anatawa12.fixrtm.nashorn.api.scripting;

import com.anatawa12.fixrtm.nashorn.internal.objects.Global;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Context;

import javax.script.ScriptContext;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The ObjectOutput to deserialize nashorn objects.
 * You have to use this class to deserialize nashorn objects.
 */
public class ScriptEngineObjectInput implements ObjectInput {
    private final ScriptEngineObjectInputStream stream;
    private final Global global;
    private final Map<String, Object> excludedMap = new HashMap<>();

    public ScriptEngineObjectInput(InputStream out, NashornScriptEngine engine) throws IOException {
        this(out, engine, engine.getContext());
    }

    public ScriptEngineObjectInput(InputStream out, NashornScriptEngine engine, ScriptContext ctxt) throws IOException {
        stream = new ScriptEngineObjectInputStream(out);
        this.global = engine.getNashornGlobalFrom(ctxt);

        ScriptEngineObjectOutput.BuildInPathUtils.addBuildIns(engine, ctxt, global, this::addExcludedObject);
    }

    /**
     * adds excluded object. the object is used to replace(resolve) objects presented by 
     * {@link ScriptEngineObjectOutput#addExcludedObject(Object, String)} to deserialize the object. 
     * the Object do not have to implement {@link Serializable}.
     * @param object the object.
     * @param name the name of the object. this must be unique.
     * @throws NullPointerException some arguments are null
     */
    public void addExcludedObject(Object object, String name) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(object, "name");
        if (excludedMap.containsKey(name))
            throw new IllegalArgumentException("'" + name + "' is already exits");
        excludedMap.put(name, object);
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return stream.read();
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return stream.read(buf, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return stream.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /** {@inheritDoc} */
    @Override
    public boolean readBoolean() throws IOException {
        return stream.readBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() throws IOException {
        return stream.readByte();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() throws IOException {
        return stream.readUnsignedByte();
    }

    /** {@inheritDoc} */
    @Override
    public char readChar() throws IOException {
        return stream.readChar();
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() throws IOException {
        return stream.readShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() throws IOException {
        return stream.readUnsignedShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() throws IOException {
        return stream.readInt();
    }

    /** {@inheritDoc} */
    @Override
    public long readLong() throws IOException {
        return stream.readLong();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() throws IOException {
        return stream.readFloat();
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() throws IOException {
        return stream.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(byte[] buf) throws IOException {
        stream.readFully(buf);
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(byte[] buf, int off, int len) throws IOException {
        stream.readFully(buf, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int skipBytes(int len) throws IOException {
        return stream.skipBytes(len);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public String readLine() throws IOException {
        return stream.readLine();
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() throws IOException {
        return stream.readUTF();
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b) throws IOException {
        return stream.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    /** {@inheritDoc} */
    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        final Global oldGlobal = Context.getGlobal();
        final boolean globalChanged = (oldGlobal != global);
        try {
            if (globalChanged) {
                Context.setGlobal(global);
            }

            return stream.readObject();
        } finally {
            if (globalChanged) {
                Context.setGlobal(oldGlobal);
            }
        }
    }

    /**
     * the ObjectOutputStream to resolve Object
     */
    public class ScriptEngineObjectInputStream extends ObjectInputStream {
        private ScriptEngineObjectInputStream(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }

        /**
         * resolves objects excluded with {@link ScriptEngineObjectInput#addExcludedObject(Object, String)}
         * from {@link ScriptEngineObjectOutput.NamedReplacedObject}
         * 
         * {@inheritDoc}
         * 
         * @return the {@link ScriptEngineObjectOutput.NamedReplacedObject} object or argument obj.
         */
        @Override
        protected Object resolveObject(Object obj) throws IOException {
            if (obj instanceof ScriptEngineObjectOutput.NamedReplacedObject) {
                String name = ((ScriptEngineObjectOutput.NamedReplacedObject) obj).getName();
                Object object = excludedMap.get(name);
                if (object == null) throw new NotSerializableException("NamedReplacedObject(" + name + "): not found");
                return object;
            }
            return obj;
        }
    }
}
