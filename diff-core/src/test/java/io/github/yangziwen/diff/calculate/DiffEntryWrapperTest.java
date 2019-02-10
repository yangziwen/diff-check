package io.github.yangziwen.diff.calculate;

import java.util.Arrays;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Edit.class })
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
        Edit edit1 = PowerMockito.mock(Edit.class);
        Edit edit2 = PowerMockito.mock(Edit.class);
        PowerMockito.when(edit1.getType()).thenReturn(Type.DELETE);
        PowerMockito.when(edit2.getType()).thenReturn(Type.DELETE);
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .editList(Arrays.asList(edit1, edit2))
                .build();
        Assert.assertTrue(wrapper.isAllDeletedEdits());
    }

}
