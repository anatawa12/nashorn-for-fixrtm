package com.anatawa12.fixrtm.nashorn.api.scripting;

import com.anatawa12.fixrtm.nashorn.internal.objects.Global;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Context;
import com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptObject;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Undefined;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.function.BiConsumer;

/**
 * The ObjectOutput to serialize nashorn objects.
 * You have to use this class to serialize nashorn objects.
 */
public class ScriptEngineObjectOutput implements ObjectOutput {
    private final ScriptEngineObjectOutputStream stream;
    private final Global global;
    private final Map<Object, String> excludedMap = new HashMap<>();
    private final Set<String> excludedNames = new HashSet<>();
    // package-private because used by ScriptEngineObjectInput
    final static Set<String> buildInObjectNames;
    final static Set<String> excludedBuildInObjectNames;

    /**
     * constructs ScriptEngineObjectOutput with default context of engine.
     * @param out the output write to.
     * @param engine the script engine.
     * @throws IOException if an I/O error occurs while writing stream header of ObjectOutput
     */
    public ScriptEngineObjectOutput(OutputStream out, NashornScriptEngine engine) throws IOException {
        this(out, engine, engine.getContext());
    }

    /**
     * constructs ScriptEngineObjectOutput with custom context of engine.
     * @param out the output write to.
     * @param engine the script engine.
     * @param ctxt the context to get standard Built-in ECMAScript Objects and Nashorn Built-in Objects. 
     * @throws IOException if an I/O error occurs while writing stream header of ObjectOutput
     */
    public ScriptEngineObjectOutput(OutputStream out, NashornScriptEngine engine, ScriptContext ctxt) throws IOException {
        stream = new ScriptEngineObjectOutputStream(out);
        this.global = engine.getNashornGlobalFrom(ctxt);

        BuildInPathUtils.addBuildIns(engine, ctxt, global, this::addExcludedObject);
    }

    /**
     * adds excluded object. you have to call {@link ScriptEngineObjectInput#addExcludedObject(Object, String)} 
     * to deserialize the object. the Object do not have to implement {@link Serializable}.
     * @param object the object to exclude.
     * @param name the name of the object. this must be unique.
     * @throws NullPointerException some arguments are null
     */
    public void addExcludedObject(Object object, String name) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(object, "name");
        if (excludedNames.contains(name))
            throw new IllegalArgumentException("'" + name + "' is already exits");
        excludedNames.add(name);
        excludedMap.put(object, name);
    }

    /** {@inheritDoc} */
    @Override
    public void write(int val) throws IOException {
        stream.write(val);
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte[] buf) throws IOException {
        stream.write(buf);
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        stream.write(buf, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /** {@inheritDoc} */
    @Override
    public void writeBoolean(boolean val) throws IOException {
        stream.writeBoolean(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeByte(int val) throws IOException {
        stream.writeByte(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeShort(int val) throws IOException {
        stream.writeShort(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeChar(int val) throws IOException {
        stream.writeChar(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeInt(int val) throws IOException {
        stream.writeInt(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeLong(long val) throws IOException {
        stream.writeLong(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeFloat(float val) throws IOException {
        stream.writeFloat(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeDouble(double val) throws IOException {
        stream.writeDouble(val);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(String str) throws IOException {
        stream.writeBytes(str);
    }

    /** {@inheritDoc} */
    @Override
    public void writeChars(String str) throws IOException {
        stream.writeChars(str);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUTF(String str) throws IOException {
        stream.writeUTF(str);
    }

    /** {@inheritDoc} */
    @Override
    public void writeObject(Object obj) throws IOException {
        final Global oldGlobal = Context.getGlobal();
        final boolean globalChanged = (oldGlobal != global);
        try {
            if (globalChanged) {
                Context.setGlobal(global);
            }

            stream.writeObject(obj);
        } finally {
            if (globalChanged) {
                Context.setGlobal(oldGlobal);
            }
        }
    }

    /**
     * the ObjectOutputStream to replace Object
     */
    public class ScriptEngineObjectOutputStream extends ObjectOutputStream {
        private ScriptEngineObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        /**
         * replaces objects excluded with {@link ScriptEngineObjectOutput#addExcludedObject(Object, String)}
         * to {@link NamedReplacedObject}
         * 
         * {@inheritDoc}
         * 
         * @return the {@link NamedReplacedObject} object or argument obj.
         */
        @Override
        protected Object replaceObject(Object obj) {
            if (obj == null) return null;
            String name = excludedMap.get(obj);
            if (name == null) return obj;
            return new NamedReplacedObject(name);
        }
    }

    /**
     * this presents excluded object.
     */
    public static class NamedReplacedObject implements Serializable {
        private String name;

        private NamedReplacedObject(String name) {
            this.name = name;
        }

        /**
         * @serialData
         * 
         * readObject contains only one readUTF invocation to read name
         */
        private void readObject(ObjectInputStream stream) throws IOException {
            name = stream.readUTF();
        }

        /**
         * @serialData
         *
         * writeObject contains only one writeUTF invocation to write name
         */
        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeUTF(name);
        }

        public String getName() {
            return name;
        }
    }

    static class BuildInPathUtils {
        public static Object get(ScriptObject object, String errorName, String buildInObjectName) {
            String[] path = buildInObjectName.split("\\.");
            Object cur = object;
            StringBuilder passedPath = new StringBuilder(errorName);
            for (String s : path) {
                if (!(cur instanceof ScriptObject))
                    throw new IllegalArgumentException(passedPath + " is not ScriptObject");
                passedPath.append('.').append(s);
                cur = ((ScriptObject) cur).get(s);
                if (cur == Undefined.getUndefined())
                    throw new IllegalArgumentException(passedPath + " is undefied");
            }
            return cur;
        }

        public static void addBuildIns(
                ScriptEngine engine, 
                ScriptContext ctxt, 
                Global global, 
                BiConsumer<Object, String> addExcludedObject) {

            for (String buildInObjectName : buildInObjectNames) {
                if (excludedBuildInObjectNames.contains(buildInObjectName)) continue;
                addExcludedObject.accept(
                        get(global, "global", buildInObjectName),
                        buildInObjectName);
            }
            addExcludedObject.accept(ctxt, "context");
            addExcludedObject.accept(engine, "engine");
        }
    }

    static {
        Set<String> buildInObjectNamesImpl = new HashSet<>();
        Set<String> excludedBuildInObjectNamesImpl = new HashSet<>();
        try {
            BuildInsReader reader = new BuildInsReader(
                    new BufferedReader(
                            new InputStreamReader(
                                    Objects.requireNonNull(ScriptEngineObjectOutput.class.getClassLoader()
                                            .getResourceAsStream("com/anatawa12/fixrtm/nashorn/" +
                                                    "api/scripting/resources/ScriptEngineObjectOutput.buildins.txt")))));
            String element;
            while ((element = reader.getNext()) != null) {
                String fullName = element.startsWith("!") ? element.substring(1) : element;
                buildInObjectNamesImpl.add(fullName);
                if (element.startsWith("!"))
                    excludedBuildInObjectNamesImpl.add(fullName);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        buildInObjectNames = Collections.unmodifiableSet(buildInObjectNamesImpl);
        excludedBuildInObjectNames = Collections.unmodifiableSet(excludedBuildInObjectNamesImpl);
    }

    private static class BuildInsReader {
        private final BufferedReader reader;
        private final List<String> scope = new ArrayList<>();
        private String lastRead;
        private int line;

        private BuildInsReader(BufferedReader reader) {
            this.reader = reader;
        }

        private String getNext() throws IOException {
            do {
                String line = reader.readLine();
                if (line == null) return end();
                this.line++;
                line = line.trim();
                if (line.equals("{")) {
                    if (lastRead == null)
                        throw new IOException("invalid format(" + this.line + ") :no identifier line before {");
                    scope.add(lastRead);
                    lastRead = null;
                    continue;
                } else if (line.equals("}")) {
                    scope.remove(scope.size() - 1);
                    lastRead = null;
                    continue;
                } else if (line.startsWith("#")) { // comment
                    continue;
                }
                lastRead = line;
                if (scope.isEmpty()) {
                    return line;
                } else {
                    boolean isExcluded = line.startsWith("!");
                    if (isExcluded) {
                        return "!" + String.join(".", scope) + "." + line.substring(1);
                    } else {
                        return String.join(".", scope) + "." + line;
                    }
                }
            } while (true);
        }

        private String end() throws IOException {
            if (!scope.isEmpty())
                throw new EOFException("unexpected EOF(" + this.line + ") : expected }");
            return null;
        }
    }
}
