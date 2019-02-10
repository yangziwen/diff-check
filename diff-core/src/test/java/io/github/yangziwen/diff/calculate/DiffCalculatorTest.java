package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class DiffCalculatorTest extends RepositoryTestCase {

    private static final String DEFAULT_USERNAME = "test";

    private static final String DEFAULT_EMAIL = "test@test.com";

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        setUp();
    }

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

            Assert.assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            Assert.assertEquals(fileToChange, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            Assert.assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            Assert.assertEquals(1, replaceEdit.getBeginB());
            Assert.assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            Assert.assertEquals(Edit.Type.INSERT, insertEdit.getType());
            Assert.assertEquals(3, insertEdit.getBeginB());
            Assert.assertEquals(4, insertEdit.getEndB());
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
            List<DiffEntryWrapper> wrappers = Whitebox.<List<DiffEntryWrapper>>invokeMethod(
                    calculator, "doCalculateCommitDiff",
                    oldCommit, newCommit, reader, git, repoDir, Collections.emptySet());

            Assert.assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            Assert.assertEquals(fileToChange, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            Assert.assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            Assert.assertEquals(1, replaceEdit.getBeginB());
            Assert.assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            Assert.assertEquals(Edit.Type.INSERT, insertEdit.getType());
            Assert.assertEquals(3, insertEdit.getBeginB());
            Assert.assertEquals(4, insertEdit.getEndB());
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
            List<DiffEntryWrapper> wrappers = Whitebox.<List<DiffEntryWrapper>>invokeMethod(
                    calculator, "doCalculateCommitDiff",
                    oldCommit, newCommit, reader, git, repoDir, Collections.emptySet());

            Assert.assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            Assert.assertEquals(fileToAdd, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit insertEdit = edits.get(0);
            Assert.assertEquals(Edit.Type.INSERT, insertEdit.getType());
            Assert.assertEquals(0, insertEdit.getBeginB());
            Assert.assertEquals(1, insertEdit.getEndB());

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
            List<DiffEntryWrapper> wrappers = Whitebox.<List<DiffEntryWrapper>>invokeMethod(
                    calculator, "doCalculateIndexedDiff",
                    oldCommit, reader, git, repoDir);

            Assert.assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            Assert.assertEquals(fileToChange, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit replaceEdit = edits.get(0);
            Assert.assertEquals(Edit.Type.REPLACE, replaceEdit.getType());
            Assert.assertEquals(1, replaceEdit.getBeginB());
            Assert.assertEquals(2, replaceEdit.getEndB());

            Edit insertEdit = edits.get(1);
            Assert.assertEquals(Edit.Type.INSERT, insertEdit.getType());
            Assert.assertEquals(3, insertEdit.getBeginB());
            Assert.assertEquals(4, insertEdit.getEndB());
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
            List<DiffEntryWrapper> wrappers = Whitebox.<List<DiffEntryWrapper>>invokeMethod(
                    calculator, "doCalculateIndexedDiff",
                    oldCommit, reader, git, repoDir);

            Assert.assertEquals(1, wrappers.size());

            DiffEntryWrapper wrapper = wrappers.get(0);
            Assert.assertEquals(fileToAdd, wrapper.getNewFile());

            List<Edit> edits = wrapper.getEditList();

            Edit insertEdit = edits.get(0);
            Assert.assertEquals(Edit.Type.INSERT, insertEdit.getType());
            Assert.assertEquals(0, insertEdit.getBeginB());
            Assert.assertEquals(1, insertEdit.getEndB());

        }

    }

    private static void writeStringToFile(File file, String content) {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RevCommit doCommit(Git git) throws Exception {
        return git.commit()
                .setAll(true)
                .setAuthor(DEFAULT_USERNAME, DEFAULT_EMAIL)
                .setCommitter(DEFAULT_USERNAME, DEFAULT_EMAIL)
                .setMessage("new commit")
                .call();
    }

}
