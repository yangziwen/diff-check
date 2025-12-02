package io.github.yangziwen.jacoco.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.jacoco.core.internal.analysis.filter.Filters;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;

public class FilterUtil {

    private FilterUtil() { }

    public static void appendFilter(IFilter filter) throws Exception {
        IFilter[] filters = getAllFilters();
        IFilter[] newFilters = new IFilter[filters.length + 1];
        System.arraycopy(filters, 0, newFilters, 0, filters.length);
        newFilters[newFilters.length - 1] = filter;
        setAllFilters(newFilters);
    }

    private static IFilter[] getAllFilters() throws Exception {
        Field filtersField = Filters.class.getDeclaredField("filters");
        filtersField.setAccessible(true);
        return (IFilter[]) filtersField.get(Filters.ALL);
    }

    private static void setAllFilters(IFilter[] filters) throws Exception {
        Field filtersField = Filters.class.getDeclaredField("filters");
        filtersField.setAccessible(true);
        Field modifiersField = filtersField.getClass().getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = filtersField.getModifiers();
        modifiersField.setInt(filtersField, modifiers & ~Modifier.FINAL);
        filtersField.set(Filters.ALL, filters);
        modifiersField.setInt(filtersField, modifiers);
    }

    /**
     * 返回类文件的路径和名称
     * @return 例如：com/example/Example.java
     */
    public static String getClassPath(IFilterContext context) {
        /*
         ASM 的 ClassReader 获取的 className 是内部名称格式（Internal Name），使用斜杠 / 作为包分隔符
         返回的斜杠格式是 JVM 规范定义的内部名称格式，不是文件系统路径。
         */
        int lastSlashIndex = context.getClassName().lastIndexOf("/");
        String path = context.getSourceFileName();
        if (lastSlashIndex >= 0) {
            path = context.getClassName().substring(0, lastSlashIndex + 1) + context.getSourceFileName();
        }
        return path;
    }

    public static List<LineNumberNodeWrapper> collectLineNumberNodeList(InsnList instructions) {
        List<LineNumberNodeWrapper> list = new ArrayList<>();
        AbstractInsnNode node = instructions.getFirst();
        while (node != instructions.getLast()) {
            if (node instanceof LineNumberNode) {
                if (CollectionUtil.isNotEmpty(list)) {
                    list.get(list.size() - 1).setNext(node);
                }
                list.add(new LineNumberNodeWrapper(LineNumberNode.class.cast(node)));
            }
            node = node.getNext();
        }
        if (CollectionUtil.isNotEmpty(list)) {
            list.get(list.size() - 1).setNext(instructions.getLast());
        }
        return list;
    }

}
