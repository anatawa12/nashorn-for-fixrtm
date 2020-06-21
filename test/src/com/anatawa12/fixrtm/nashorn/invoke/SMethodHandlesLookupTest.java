package com.anatawa12.fixrtm.nashorn.invoke;

import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.testng.Assert.*;

public class SMethodHandlesLookupTest {
    @Test
    public void wrappedLookupForPublicLookupShouldBeSame() {
        // only if MethodHandles.publicLookup() returns same instance.
        // the openjdk returns same instance but it is not documented.
        if (MethodHandles.publicLookup() == MethodHandles.publicLookup()) {
            assertSame(SMethodHandles.publicLookup(), SMethodHandles.l(MethodHandles.publicLookup()));
        }
    }


    @Test
    public void getDirectConstructor() throws NoSuchMethodException, IllegalAccessException {
        SMethodHandles.publicLookup().findConstructor(SamePackagePublicTestClass.class, 
                MethodType.methodType(void.class));
    }

    @Test
    public void getDirectMethod() throws NoSuchMethodException, IllegalAccessException {
        SMethodHandles.publicLookup().findVirtual(SamePackagePublicTestClass.class, "method", 
                MethodType.methodType(void.class));
    }

    @Test
    public void getDirectFieldGetter() throws NoSuchFieldException, IllegalAccessException {
        SMethodHandles.publicLookup().findGetter(SamePackagePublicTestClass.class, "field", 
                int.class);
    }

    @Test
    public void getDirectFieldSetter() throws NoSuchFieldException, IllegalAccessException {
        SMethodHandles.publicLookup().findSetter(SamePackagePublicTestClass.class, "field", 
                int.class);
    }

    @Test
    public void getDirectStaticMethod() throws NoSuchMethodException, IllegalAccessException {
        SMethodHandles.publicLookup().findStatic(SamePackagePublicTestClass.class, "staticMethod", 
                MethodType.methodType(void.class));
    }

    @Test
    public void getDirectStaticFieldGetter() throws NoSuchFieldException, IllegalAccessException {
        SMethodHandles.publicLookup().findStaticGetter(SamePackagePublicTestClass.class, "staticField", 
                int.class);
    }

    @Test
    public void getDirectStaticFieldSetter() throws NoSuchFieldException, IllegalAccessException {
        SMethodHandles.publicLookup().findStaticSetter(SamePackagePublicTestClass.class, "staticField",
                int.class);
    }
}

