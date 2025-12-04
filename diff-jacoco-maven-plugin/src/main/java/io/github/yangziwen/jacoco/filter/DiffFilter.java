package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jgit.diff.Edit;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import io.github.yangziwen.jacoco.util.FilterUtil;
import io.github.yangziwen.jacoco.util.LineNumberNodeWrapper;

public class DiffFilter implements IFilter {

    private static final String SOURCE_PATH_PREFIX = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;

    private Map<String, DiffEntryWrapper> classPathDiffEntryMap = new HashMap<>();

    public DiffFilter(MavenProject project, File gitDir, List<DiffEntryWrapper> entries) {
        File baseDir = project.getBasedir();
        String baseDirPath = baseDir.getAbsolutePath();

        for (DiffEntryWrapper entry : entries) {
            String absolutePath = entry.getAbsoluteNewPath();
            if (!absolutePath.startsWith(baseDirPath)) {
                continue;
            }

            // 去掉项目根目录前缀
            String name = StringUtils.replaceOnce(absolutePath, baseDirPath, "");

            // 直接查找 /src/main/java/ 的位置，支持任意深度的嵌套模块
            // 这种方式比逐级去除路径段更高效，且逻辑更清晰
            int srcIndex = name.indexOf(SOURCE_PATH_PREFIX);
            if (srcIndex < 0) {
                // 不是 Java 源文件，跳过
                continue;
            }

            // 截取从 /src/main/java/ 之后的部分，得到类的相对路径
            name = name.substring(srcIndex + SOURCE_PATH_PREFIX.length());

            // 转换为 JVM 内部格式（使用 / 作为分隔符）
            name = name.replace(File.separator, "/");

            // name 与 FilterUtil.getClassPath 一致才能获取到
            classPathDiffEntryMap.put(name, entry);
        }
    }

    @Override
    public void filter(
            MethodNode methodNode,
            IFilterContext context,
            IFilterOutput output) {

        String classPath = FilterUtil.getClassPath(context);

        InsnList instructions = methodNode.instructions;

        DiffEntryWrapper wrapper = classPathDiffEntryMap.get(classPath);

        if (wrapper == null) {
            output.ignore(instructions.getFirst(), instructions.getLast());
            return;
        }

        List<LineNumberNodeWrapper> nodeWrapperList = FilterUtil.collectLineNumberNodeList(instructions);

        for (Edit edit : wrapper.getEditList()) {
            if (edit.getType() != Edit.Type.INSERT && edit.getType() != Edit.Type.REPLACE) {
                continue;
            }
            for (LineNumberNodeWrapper nodeWrapper : nodeWrapperList) {
                int line = nodeWrapper.getLine();
                if (line > edit.getBeginB() && line <= edit.getEndB()) {
                    nodeWrapper.setIgnored(false);
                }
            }
        }

        for (LineNumberNodeWrapper nodeWrapper : nodeWrapperList) {
            if (nodeWrapper.isIgnored()) {
                output.ignore(nodeWrapper.getNode(), nodeWrapper.getNext());
            }
        }

    }

}
