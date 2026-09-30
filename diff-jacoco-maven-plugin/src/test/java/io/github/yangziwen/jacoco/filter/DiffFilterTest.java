package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;

public class DiffFilterTest {

    @Test
    public void ignoreLinesOutOfDiffRange() throws Exception {
        File baseDir = Files.createTempDirectory("diff-filter-test").toFile().getCanonicalFile();

        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(baseDir).when(project).getBasedir();

        DiffEntryWrapper wrapper = newWrapper(baseDir, "src/main/java/com/example/Foo.java",
                new Edit(0, 5, 8, 12));

        DiffFilter filter = new DiffFilter(Collections.singletonList(project), Collections.singletonList(wrapper));

        MethodNode methodNode = buildMethodNode(10, 20);
        IFilterContext context = buildContext("com/example/Foo", "Foo.java");
        IFilterOutput output = Mockito.mock(IFilterOutput.class);

        filter.filter(methodNode, context, output);

        // 第 10 行落在变更区间 (8, 12] 内不应被 ignore，第 20 行在区间外应被 ignore
        ArgumentCaptor<org.objectweb.asm.tree.AbstractInsnNode> startCaptor =
                ArgumentCaptor.forClass(org.objectweb.asm.tree.AbstractInsnNode.class);
        Mockito.verify(output, Mockito.times(1)).ignore(startCaptor.capture(), any());
        assertSame(findLineNumberNode(methodNode, 20), startCaptor.getValue());
    }

    @Test
    public void ignoreWholeMethodForClassOutOfDiff() throws Exception {
        File baseDir = Files.createTempDirectory("diff-filter-test").toFile().getCanonicalFile();

        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(baseDir).when(project).getBasedir();

        DiffFilter filter = new DiffFilter(Collections.singletonList(project), Collections.emptyList());

        MethodNode methodNode = buildMethodNode(10, 20);
        IFilterContext context = buildContext("com/example/Bar", "Bar.java");
        IFilterOutput output = Mockito.mock(IFilterOutput.class);

        filter.filter(methodNode, context, output);

        Mockito.verify(output, Mockito.times(1))
                .ignore(methodNode.instructions.getFirst(), methodNode.instructions.getLast());
    }

    @Test
    public void ignoreFilesNotUnderSrcMainJava() throws Exception {
        File baseDir = Files.createTempDirectory("diff-filter-test").toFile().getCanonicalFile();

        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(baseDir).when(project).getBasedir();

        // src/test/java 下的文件不属于 diff 覆盖范围
        DiffEntryWrapper wrapper = newWrapper(baseDir, "src/test/java/com/example/Foo.java",
                new Edit(0, 5, 8, 12));

        DiffFilter filter = new DiffFilter(Collections.singletonList(project), Collections.singletonList(wrapper));

        MethodNode methodNode = buildMethodNode(10, 20);
        IFilterContext context = buildContext("com/example/Foo", "Foo.java");
        IFilterOutput output = Mockito.mock(IFilterOutput.class);

        filter.filter(methodNode, context, output);

        Mockito.verify(output, Mockito.times(1))
                .ignore(methodNode.instructions.getFirst(), methodNode.instructions.getLast());
    }

    private DiffEntryWrapper newWrapper(File gitDir, String newPath, Edit edit) {
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .gitDir(gitDir)
                .diffEntry(new DummyDiffEntry(newPath))
                .editList(Collections.singletonList(edit))
                .build();
        return wrapper;
    }

    private MethodNode buildMethodNode(int... lines) {
        MethodNode methodNode = new MethodNode();
        methodNode.instructions = new InsnList();
        for (int line : lines) {
            methodNode.instructions.add(new LineNumberNode(line, new LabelNode()));
            methodNode.instructions.add(new LabelNode());
            methodNode.instructions.add(new VarInsnNode(1, 1));
        }
        return methodNode;
    }

    private IFilterContext buildContext(String className, String sourceFileName) {
        IFilterContext context = Mockito.mock(IFilterContext.class);
        Mockito.doReturn(className).when(context).getClassName();
        Mockito.doReturn(sourceFileName).when(context).getSourceFileName();
        return context;
    }

    private org.objectweb.asm.tree.AbstractInsnNode findLineNumberNode(MethodNode methodNode, int line) {
        for (org.objectweb.asm.tree.AbstractInsnNode node : methodNode.instructions) {
            if (node instanceof LineNumberNode && ((LineNumberNode) node).line == line) {
                return node;
            }
        }
        throw new IllegalArgumentException("no line number node for line " + line);
    }

    private static class DummyDiffEntry extends DiffEntry {

        private final String filePath;

        DummyDiffEntry(String filePath) {
            super();
            this.filePath = filePath;
        }

        @Override
        public String getNewPath() {
            return filePath;
        }

    }

}
