package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jgit.diff.Edit;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;

public class DiffFilter implements IFilter {

    private static final String SOURCE_PATH_PREFIX = "/src/main/java/";

    private Map<String, DiffEntryWrapper> classPathDiffEntryMap = new HashMap<>();

    public DiffFilter(MavenProject project, File gitDir, List<DiffEntryWrapper> entries) {
        File baseDir = project.getBasedir();
        String baseDirPath = baseDir.getAbsolutePath();
        @SuppressWarnings("unchecked")
        List<String> modules = project.getModules();
        for (DiffEntryWrapper entry : entries) {
            if (!entry.getAbsoluteNewPath().startsWith(baseDirPath)) {
                continue;
            }
            String name = StringUtils.replaceOnce(entry.getAbsoluteNewPath(), baseDirPath, "");
            if (CollectionUtils.isNotEmpty(modules)) {
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

        String classPath = getClassPath(context);

        InsnList instructions = methodNode.instructions;

        DiffEntryWrapper wrapper = classPathDiffEntryMap.get(classPath);

        if (wrapper == null) {
            output.ignore(instructions.getFirst(), instructions.getLast());
            return;
        }

        List<LineNumberNodeWrapper> nodeWrapperList = collectLineNumberNodeList(instructions);

        for (Edit edit : wrapper.getEditList()) {
            if (edit.getType() != Edit.Type.INSERT && edit.getType() != Edit.Type.REPLACE) {
                continue;
            }
            for (LineNumberNodeWrapper nodeWrapper : nodeWrapperList) {
                int line = nodeWrapper.getLine();
                if (line >= edit.getBeginB() && line <= edit.getEndB()) {
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

    private List<LineNumberNodeWrapper> collectLineNumberNodeList(InsnList instructions) {
        List<LineNumberNodeWrapper> list = new ArrayList<>();
        AbstractInsnNode node = instructions.getFirst();
        while (node != instructions.getLast()) {
            if (node instanceof LineNumberNode) {
                if (CollectionUtils.isNotEmpty(list)) {
                    list.get(list.size() - 1).setNext(node);
                }
                list.add(new LineNumberNodeWrapper(LineNumberNode.class.cast(node)));
            }
            node = node.getNext();
        }
        if (CollectionUtils.isNotEmpty(list)) {
            list.get(list.size() - 1).setNext(instructions.getLast());
        }
        return list;
    }

    private String getClassPath(IFilterContext context) {
        int lastSlashIndex = context.getClassName().lastIndexOf(File.separator);
        String path = context.getSourceFileName();
        if (lastSlashIndex >= 0) {
            path = context.getClassName().substring(0, lastSlashIndex + 1) + context.getSourceFileName();
        }
        return path;
    }

    static class LineNumberNodeWrapper {

        private LineNumberNode node;

        private AbstractInsnNode next;

        private boolean ignored = true;

        LineNumberNodeWrapper(LineNumberNode node) {
            this.node = node;
        }

        public int getLine() {
            return getNode().line;
        }

        public LineNumberNode getNode() {
            return node;
        }

        public AbstractInsnNode getNext() {
            return next;
        }

        public void setNext(AbstractInsnNode next) {
            this.next = next;
        }

        public boolean isIgnored() {
            return ignored;
        }

        public void setIgnored(boolean ignored) {
            this.ignored = ignored;
        }

    }

}
