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

/**
 * jacoco 内部 filter 框架的工具类。
 *
 * <p><b>版本锁定：本类与 jacoco 0.8.7 强绑定，升级 jacoco 或 JDK 前必须先适配本类。</b></p>
 *
 * <p>注入机制依赖两个 0.8.7 时代的实现细节：</p>
 * <ul>
 *   <li><b>同包名 shadow 类</b>：本模块内置了
 *       {@code org.jacoco.core.internal.analysis.filter.Filters} 的同包名副本
 *       （见 src/main/java/org/jacoco/core/internal/analysis/filter/Filters.java），
 *       利用插件类加载优先级覆盖 jacoco core 中的同名类，
 *       使分析器走我们带 {@code ALL} 静态字段的版本。
 *       该副本的 filter 清单固定为 0.8.7 的 24 个内置 filter，
 *       升级 jacoco 时必须同步增补（新版还有 ExhaustiveSwitchFilter、
 *       RecordPatternFilter、KotlinComposeFilter 等），否则新版本自带
 *       的 filter 会被 shadow 类整体旁路；</li>
 *   <li><b>反射换数组</b>：{@code Filters} 的私有字段 {@code filters}
 *       （{@code IFilter[]} 数组）为 final，通过反射修改
 *       {@code java.lang.reflect.Field} 的 {@code modifiers} 绕过后替换。
 *       该 hack 仅在 JDK 8 ~ 11 可用，JDK 12+ 因反射字段过滤
 *       （field filtering）无法获取 {@code modifiers}，会直接抛
 *       {@link NoSuchFieldException}（如需支持 JDK 12+，可改用
 *       {@code sun.misc.Unsafe} 直接写静态字段）。</li>
 * </ul>
 *
 * <p>因此本模块的兼容性约束为：<b>jacoco = 0.8.7，JDK 8 ~ 11</b>。
 * jacoco 0.8.8 起官方已删除 {@code ALL} 字段（改为 {@code NONE} + {@code all()}
 * 工厂），0.8.15+ 更是无状态静态工厂设计，上述机制均需重写。
 * 升级前请先确认 shadow 类与反射链路已适配，并重点回归
 * {@code DiffAgentMojo} 在真实构建中的 filter 注入行为
 * （当前单元测试未覆盖 appendFilter 的反射链路）。</p>
 */
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
