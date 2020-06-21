package com.anatawa12.fixrtm.nashorn.api.scripting;

import com.anatawa12.fixrtm.nashorn.internal.objects.Global;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Context;
import com.anatawa12.fixrtm.nashorn.internal.runtime.ScriptObject;
import com.anatawa12.fixrtm.nashorn.internal.runtime.Undefined;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

public class ScriptEngineObjectOutputTestNoSecurity {
    @Test
    public void buildInObjectNamesAreContainInGlobal() {
        assertEquals(ScriptEngineObjectOutputTestNoSecurity.class.getClassLoader(), ScriptEngineObjectOutput.class.getClassLoader());
        NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
        Global global = engine.getNashornGlobalFrom(engine.getContext());
        final Global oldGlobal = Context.getGlobal();
        final boolean globalChanged = (oldGlobal != global);
        try {
            if (globalChanged) {
                Context.setGlobal(global);
            }

            Map<Object, String> foundObjects = new IdentityHashMap<>();

            for (String buildInObjectName : ScriptEngineObjectOutput.buildInObjectNames) {
                foundObjects.put(
                        ScriptEngineObjectOutput.BuildInPathUtils.get(global, "global", buildInObjectName), 
                        buildInObjectName);
            }

            Set<Object> prceessed = Collections.newSetFromMap(new IdentityHashMap<>());
            checkAllObjectsAreContainInMap(global, "global", foundObjects, prceessed);

        } finally {
            if (globalChanged) {
                Context.setGlobal(oldGlobal);
            }
        }
    }

    @Test
    public void excludedBuildInObjectNamesAreNotScriptObject() {
        assertEquals(ScriptEngineObjectOutputTestNoSecurity.class.getClassLoader(), ScriptEngineObjectOutput.class.getClassLoader());
        NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
        Global global = engine.getNashornGlobalFrom(engine.getContext());
        final Global oldGlobal = Context.getGlobal();
        final boolean globalChanged = (oldGlobal != global);
        try {
            if (globalChanged) {
                Context.setGlobal(global);
            }

            for (String buildInObjectName : ScriptEngineObjectOutput.excludedBuildInObjectNames) {
                Object cur = ScriptEngineObjectOutput.BuildInPathUtils.get(global, "global", buildInObjectName);

                if (cur instanceof ScriptObject)
                    fail(buildInObjectName + " is ScriptObject, actual: " + cur.getClass());
            }
        } finally {
            if (globalChanged) {
                Context.setGlobal(oldGlobal);
            }
        }
    }

    @Test
    public void includedBuildInObjectNamesAreNotScriptObject() {
        assertEquals(ScriptEngineObjectOutputTestNoSecurity.class.getClassLoader(), ScriptEngineObjectOutput.class.getClassLoader());
        NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
        Global global = engine.getNashornGlobalFrom(engine.getContext());
        final Global oldGlobal = Context.getGlobal();
        final boolean globalChanged = (oldGlobal != global);
        try {
            if (globalChanged) {
                Context.setGlobal(global);
            }

            for (String buildInObjectName : ScriptEngineObjectOutput.buildInObjectNames) {
                if (ScriptEngineObjectOutput.excludedBuildInObjectNames.contains(buildInObjectName)) continue;
                Object cur = ScriptEngineObjectOutput.BuildInPathUtils.get(global, "global", buildInObjectName);

                if (!(cur instanceof ScriptObject))
                    fail(buildInObjectName + " is not ScriptObject, actual: " + cur.getClass());
            }
        } finally {
            if (globalChanged) {
                Context.setGlobal(oldGlobal);
            }
        }
    }

    private void checkAllObjectsAreContainInMap(ScriptObject object, String path, Map<Object, String> foundObjects, Set<Object> prceessed) {
        for (Object key : object.getMap()) {
            Object value = object.get(key);
            if (prceessed.contains(value)) continue;
            prceessed.add(value);
            if (!foundObjects.containsKey(value))
                fail(path + "." + key + ": " + value + "(" + value.getClass().getSimpleName() + "): not found.");
            if (value instanceof ScriptObject) {
                checkAllObjectsAreContainInMap((ScriptObject) value, path + "." + key, foundObjects, prceessed);
            }
        }
    }
}
