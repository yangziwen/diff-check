package io.github.yangziwen.jacoco.util;

import java.io.File;
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
 * <p><b>版本锁定：本类与 jacoco 0.8.15 强绑定，升级 jacoco 前必须先适配
 * shadow 类与 FilterUtil（及配套单测），并核对内置 filter 清单。</b></p>
 *
 * <p>注入机制（0.8.15 起）：依赖同包名 shadow 类
 * {@code org.jacoco.core.internal.analysis.filter.Filters}（见 src/main/java
 * 下同名文件），利用插件类加载优先级遮蔽 jacoco core 中的同名类。
 * 相比 0.8.7 时代的“反射私有 filters 字段 + Field.modifiers 绕过 final”，
 * shadow 类自带 {@code addFilter} 注册表，{@code all()} 每次组装时
 * 都会重新读取注册表，注入只需一次普通静态方法调用：</p>
 * <ul>
 *   <li>无反射、无堆 hack，JDK 8+ 均可用（旧机制的 Field.modifiers hack
 *       在 JDK 12+ 已失效）；</li>
 *   <li>shadow 类的内置 filter 清单锁死为特定版本，升级 jacoco 时必须
 *       对照官方 Filters.java 逐项同步，否则官方新增的内置 filter
 *       会被整体旁路；</li>
 *   <li>注册表 {@code Filters.all()} 由 Analyzer 每分析一个类调用一次，
 *       因此注册必须发生在 report 阶段分析开始前
 *       （DiffAgentMojo 在 INITIALIZE 阶段完成，天然满足）。</li>
 * </ul>
 */
public class FilterUtil {

    private FilterUtil() { }

    public static void appendFilter(IFilter filter) {
        Filters.addFilter(filter);
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
