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
import io.github.yangziwen.jacoco.util.CollectionUtil;
import io.github.yangziwen.jacoco.util.FilterUtil;
import io.github.yangziwen.jacoco.util.LineNumberNodeWrapper;

public class DiffFilter implements IFilter {

    private static final String SOURCE_PATH_PREFIX = "/src/main/java/";

    private Map<String, DiffEntryWrapper> classPathDiffEntryMap = new HashMap<>();

    public DiffFilter(MavenProject project, File gitDir, List<DiffEntryWrapper> entries) {
        File baseDir = project.getBasedir();
        String baseDirPath = baseDir.getAbsolutePath();
        List<String> modules = project.getModules();
        for (DiffEntryWrapper entry : entries) {
            if (!entry.getAbsoluteNewPath().startsWith(baseDirPath)) {
                continue;
            }
            String name = StringUtils.replaceOnce(entry.getAbsoluteNewPath(), baseDirPath, "");
            if (CollectionUtil.isNotEmpty(modules)) {
                for (String module : modules) {
                    if (name.startsWith("/" + module)) {
                        name = StringUtils.replaceOnce(name, "/" + module, "");
                        break;
                    }
                }
            }
            if (!name.startsWith(SOURCE_PATH_PREFIX)) {
                continue;
            }
            name = StringUtils.replaceOnce(name, SOURCE_PATH_PREFIX, "");
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
