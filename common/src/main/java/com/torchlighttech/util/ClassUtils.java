package com.torchlighttech.util;

/**
 * Created by caleb on 5/12/17.
 */

public class ClassUtils {
    public static String getSimpleClassNameForObject(Object some_object) {
        return ClassUtils.getSimpleClassNameForClass(some_object.getClass(), true);
    }

    public static String getSimpleClassNameForClass(Class<?> some_class,boolean stripSuffixes) {
        String pageFullClassName = some_class.getName();
        return getSimpleClassNameForClassName(pageFullClassName, stripSuffixes);
    }

    public static String getSimpleClassNameForClassName(String pageFullClassName, boolean stripSuffixes) {
        String pageClassName = "";
        int lastDoxIndex = pageFullClassName.lastIndexOf('.');
        if (lastDoxIndex > 0) {
            pageClassName = pageFullClassName.substring(lastDoxIndex + 1);
        } else {
            pageClassName = pageFullClassName;
        }
        if (stripSuffixes) {
            return pageClassName.replaceFirst("Activity$", "");
        }
        return pageClassName;
    }
}
