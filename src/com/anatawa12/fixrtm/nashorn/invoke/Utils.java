package com.anatawa12.fixrtm.nashorn.invoke;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class Utils {
    public interface PrivilegedAction<T, E extends Exception> {
        T run() throws E;
    }

    public static <T, E extends Exception> T doPrivileged(PrivilegedAction<T, E> action) throws E {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<T>) action::run);
        } catch (PrivilegedActionException e) {
            //noinspection unchecked
            throw (E) e.getException();
        }
    }

    public static void doPrivileged(Runnable action) {
        doPrivileged(() -> {
            action.run();
            return null;
        });
    }
}
