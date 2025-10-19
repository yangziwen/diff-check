package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.Edit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DiffEntryWrapperTest {

    @Test
    public void testIsDeleted() {
        DiffEntry diffEntry = Mockito.mock(DiffEntry.class);
        Mockito.when(diffEntry.getChangeType()).thenReturn(ChangeType.DELETE);
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .diffEntry(diffEntry)
                .build();
        Assert.assertTrue(wrapper.isDeleted());
    }

    @Test
    public void testIsAllDeletedEdits() {
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .editList(Arrays.asList(buildDelete(), buildDelete()))
                .build();
        Assert.assertTrue(wrapper.isAllDeletedEdits());
    }

    @Test
    public void getAbsoluteNewPath() throws IOException {
        String file = "file";
        String directory = "/some/directory";
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .gitDir(new File(directory))
                .diffEntry(new DummyDiffEntry(file))
                .build();
        String result = wrapper.getAbsoluteNewPath();
        Assert.assertEquals(new File(directory, file).getCanonicalPath(), result);
    }

    @Test
    public void getAbsoluteNewPathWithRelativeGitDir() throws IOException {
        String file = "file";
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .gitDir(new File("."))
                .diffEntry(new DummyDiffEntry(file))
                .build();
        String result = wrapper.getAbsoluteNewPath();
        Assert.assertEquals(new File(".", file).getCanonicalPath(), result);
    }

    @Test
    public void getAbsoluteNewPathException() throws IOException {
        File mock = Mockito.mock(File.class);
        Mockito.when(mock.getCanonicalPath()).thenThrow(new IOException(""));
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .gitDir(mock)
                .diffEntry(new DummyDiffEntry(""))
                .build();
        String result = wrapper.getAbsoluteNewPath();
        Assert.assertEquals("/", result);
    }

    // A "delete" edit is one where: beginA < endA && beginB == endB
    private static Edit buildDelete() {
        return new Edit(5, 10, 8, 8);
    }

    private class DummyDiffEntry extends DiffEntry {

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
