package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffCalculatorTest extends BaseCalculatorTest {

    private static final Person DEFAULT_USER = Person.builder()
            .name("test")
            .email("test@test.com")
            .build();

    @Test
    public void testCalculateDiff() throws Exception {
        try (Git git = new Git(db);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            File repoDir = git.getRepository().getDirectory().getParentFile();
            File fileToChange = new File(repoDir, "changed.txt");
            File fileRemainUnchanged = new File(repoDir, "unchanged.txt");
            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .toString());
            writeStringToFile(fileRemainUnchanged, "no change");
            git.add()
                .addFilepattern(fileToChange.getName())
                .addFilepattern(fileRemainUnchanged.getName())
                .call();
            RevCommit oldCommit = doCommit(git);

            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line changed")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .append("fourth line")
                    .toString());
            git.add().addFilepattern(fileToChange.getName()).call();
            RevCommit newCommit = doCommit(git);

            DiffCalculator calculator = DiffCalculator.builder()
                    .diffAlgorithm(new HistogramDiff())
                    .comparator(RawTextComparator.DEFAULT)
                    .bigFileThreshold(DiffHelper.DEFAULT_BIG_FILE_THRESHOLD)
                    .build();
            List<DiffEntryWrapper> wrappers = calculator.calculateDiff(
                    repoDir, oldCommit.name(), newCommit.name(), true);

            assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            assertEquals(fileToChange.getAbsolutePath(), wrapper.getNewFile().getAbsolutePath());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            assertEquals(1, replaceEdit.getBeginB());
            assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            assertEquals(Edit.Type.INSERT, insertEdit.getType());
            assertEquals(3, insertEdit.getBeginB());
            assertEquals(4, insertEdit.getEndB());
        }

    }

    @Test
    public void testDoCalculateCommitDiffWhenFileChanged() throws Exception {
        try (Git git = new Git(db);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            File repoDir = git.getRepository().getDirectory().getParentFile();
            File fileToChange = new File(repoDir, "changed.txt");
            File fileRemainUnchanged = new File(repoDir, "unchanged.txt");
            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .toString());
            writeStringToFile(fileRemainUnchanged, "no change");
            git.add()
                .addFilepattern(fileToChange.getName())
                .addFilepattern(fileRemainUnchanged.getName())
                .call();
            RevCommit oldCommit = doCommit(git);

            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line changed")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .append("fourth line")
                    .toString());
            git.add().addFilepattern(fileToChange.getName()).call();
            RevCommit newCommit = doCommit(git);

            DiffCalculator calculator = DiffCalculator.builder()
                    .diffAlgorithm(new HistogramDiff())
                    .build();
            List<DiffEntryWrapper> wrappers = invokeMethod(calculator, "doCalculateCommitDiff",
                    oldCommit, newCommit, reader, git, repoDir, Collections.emptySet());

            assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            assertEquals(fileToChange.getAbsolutePath(), wrapper.getNewFile().getAbsolutePath());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            assertEquals(1, replaceEdit.getBeginB());
            assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            assertEquals(Edit.Type.INSERT, insertEdit.getType());
            assertEquals(3, insertEdit.getBeginB());
            assertEquals(4, insertEdit.getEndB());
        }

    }

    @Test
    public void testDoCalculateCommitDiffWhenFileAdded() throws Exception {
        try (Git git = new Git(db);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            File repoDir = git.getRepository().getDirectory().getParentFile();
            File fileToAdd = new File(repoDir, "added.txt");
            File fileRemainUnchanged = new File(repoDir, "unchanged.txt");
            writeStringToFile(fileRemainUnchanged, "no change");
            git.add()
                .addFilepattern(fileRemainUnchanged.getName())
                .call();
            RevCommit oldCommit = doCommit(git);

            writeStringToFile(fileToAdd, "add file");
            git.add().addFilepattern(fileToAdd.getName()).call();
            RevCommit newCommit = doCommit(git);

            DiffCalculator calculator = DiffCalculator.builder()
                    .diffAlgorithm(new HistogramDiff())
                    .build();
            List<DiffEntryWrapper> wrappers = invokeMethod(calculator, "doCalculateCommitDiff",
                    oldCommit, newCommit, reader, git, repoDir, Collections.emptySet());

            assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            assertEquals(fileToAdd, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit insertEdit = edits.get(0);
            assertEquals(Edit.Type.INSERT, insertEdit.getType());
            assertEquals(0, insertEdit.getBeginB());
            assertEquals(1, insertEdit.getEndB());

        }

    }

    @Test
    public void testDoCalculateIndexedDiffWhenFileChanged() throws Exception {
        try (Git git = new Git(db);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            File repoDir = git.getRepository().getDirectory().getParentFile();
            File fileToChange = new File(repoDir, "changed.txt");
            File fileRemainUnchanged = new File(repoDir, "unchanged.txt");
            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .toString());
            writeStringToFile(fileRemainUnchanged, "no change");
            git.add()
                .addFilepattern(fileToChange.getName())
                .addFilepattern(fileRemainUnchanged.getName())
                .call();
            RevCommit oldCommit = doCommit(git);

            writeStringToFile(fileToChange, new StringBuilder()
                    .append("first line")
                    .append("\n")
                    .append("second line changed")
                    .append("\n")
                    .append("third line")
                    .append("\n")
                    .append("fourth line")
                    .toString());
            git.add().addFilepattern(fileToChange.getName()).call();

            DiffCalculator calculator = DiffCalculator.builder()
                    .diffAlgorithm(new HistogramDiff())
                    .build();
            List<DiffEntryWrapper> wrappers = invokeMethod(calculator, "doCalculateIndexedDiff",
                    oldCommit, reader, git, repoDir);

            assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            assertEquals(fileToChange.getAbsolutePath(), wrapper.getNewFile().getAbsolutePath());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            assertEquals(1, replaceEdit.getBeginB());
            assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            assertEquals(Edit.Type.INSERT, insertEdit.getType());
            assertEquals(3, insertEdit.getBeginB());
            assertEquals(4, insertEdit.getEndB());
        }

    }

    @Test
    public void testDoCalculateIndexedDiffWhenFileAdded() throws Exception {
        try (Git git = new Git(db);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            File repoDir = git.getRepository().getDirectory().getParentFile();
            File fileToAdd = new File(repoDir, "added.txt");
            File fileRemainUnchanged = new File(repoDir, "unchanged.txt");
            writeStringToFile(fileRemainUnchanged, "no change");
            git.add()
                .addFilepattern(fileRemainUnchanged.getName())
                .call();
            RevCommit oldCommit = doCommit(git);

            writeStringToFile(fileToAdd, "add file");
            git.add().addFilepattern(fileToAdd.getName()).call();

            DiffCalculator calculator = DiffCalculator.builder()
                    .diffAlgorithm(new HistogramDiff())
                    .build();
            List<DiffEntryWrapper> wrappers = invokeMethod(calculator, "doCalculateIndexedDiff",
                    oldCommit, reader, git, repoDir);

            assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            assertEquals(fileToAdd.getAbsolutePath(), wrapper.getAbsoluteNewPath());

            List<Edit> edits = wrapper.getEditList();

            Edit insertEdit = edits.get(0);
            assertEquals(Edit.Type.INSERT, insertEdit.getType());
            assertEquals(0, insertEdit.getBeginB());
            assertEquals(1, insertEdit.getEndB());

        }

    }

    private RevCommit doCommit(Git git) throws Exception {
        return super.doCommit(git, DEFAULT_USER, DEFAULT_USER, "new commit");
    }

    /** 替代 powermock Whitebox 的私有方法反射调用工具 */
    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Object target, String methodName, Object... args) throws Exception {
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return (T) method.invoke(target, args);
                }
            }
        }
        throw new NoSuchMethodException(methodName);
    }

}
