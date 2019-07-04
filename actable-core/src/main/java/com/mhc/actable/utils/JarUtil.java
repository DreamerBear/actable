package com.mhc.actable.utils;


import com.mhc.actable.annotation.Table;
import javafx.util.Pair;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jar工具
 *
 * @author xuchao
 */
public class JarUtil {

    /**
     * 加载本机路径的jar包,扫描出带@Table注解的类列表
     *
     * @param jarLocalPath jar包本机路径
     * @return <类加载器, 带@Table注解的类列表>
     * @throws Exception
     */
    public static Pair<ClassLoader, List<Class>> fetchEntityClassesFromJar(String jarLocalPath) throws Exception {
        if (jarLocalPath == null || jarLocalPath.length() <= 0) {
            throw new IllegalArgumentException("jarLocalPath cannot be blank");
        }
        List<Class> entityClasses = new ArrayList<>();

        JarFile jarFile = new JarFile(jarLocalPath);
        File file = new File(jarLocalPath);
        URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, Thread.currentThread().getContextClassLoader());

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            final JarEntry jarEntry = jarEntries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                String className = jarEntry.getName().replaceAll(File.separator, ".").substring(0, jarEntry.getName().length() - 6);
                Class<?> cls = classLoader.loadClass(className);
                if (cls.getAnnotation(Table.class) != null) {
                    entityClasses.add(cls);
                }
            }
        }

        return new Pair(classLoader, entityClasses);
    }
}
