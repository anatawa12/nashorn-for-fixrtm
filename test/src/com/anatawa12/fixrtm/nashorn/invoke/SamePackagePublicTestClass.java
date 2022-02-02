package com.anatawa12.fixrtm.nashorn.invoke;

public class SamePackagePublicTestClass {
    private static SamePackagePublicTestClass INSTANCE = new SamePackagePublicTestClass();
    public SamePackagePublicTestClass() {} // constructor
    public void method() {}
    public int field = 0;
    public static void staticMethod() {}
    public static int staticField = 0;
}
