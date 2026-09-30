package io.github.yangziwen.pmd.filter;

import java.util.Collections;

import org.eclipse.jgit.diff.Edit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import net.sourceforge.pmd.RuleViolation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiffLineFilterTest {

    @Test
    public void acceptViolationOnChangedLines() {
        // Edit(0, 5, 8, 12) 表示新文件第 9~12 行是变更行
        Edit edit = new Edit(0, 5, 8, 12);
        DiffEntryWrapper wrapper = Mockito.mock(DiffEntryWrapper.class);
        Mockito.doReturn("/repo/src/main/java/Sample.java").when(wrapper).getAbsoluteNewPath();
        Mockito.doReturn(Collections.singletonList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Collections.singletonList(wrapper));

        RuleViolation violation = Mockito.mock(RuleViolation.class);
        Mockito.doReturn("/repo/src/main/java/Sample.java").when(violation).getFilename();
        Mockito.doReturn(10).when(violation).getBeginLine();
        Mockito.doReturn(10).when(violation).getEndLine();

        assertTrue(filter.accept(violation));
    }

    @Test
    public void rejectViolationOnUnchangedLines() {
        Edit edit = new Edit(0, 5, 8, 12);
        DiffEntryWrapper wrapper = Mockito.mock(DiffEntryWrapper.class);
        Mockito.doReturn("/repo/src/main/java/Sample.java").when(wrapper).getAbsoluteNewPath();
        Mockito.doReturn(Collections.singletonList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Collections.singletonList(wrapper));

        RuleViolation violation = Mockito.mock(RuleViolation.class);
        Mockito.doReturn("/repo/src/main/java/Sample.java").when(violation).getFilename();
        Mockito.doReturn(20).when(violation).getBeginLine();
        Mockito.doReturn(20).when(violation).getEndLine();

        assertFalse(filter.accept(violation));
    }

    @Test
    public void rejectViolationOnFileOutOfDiff() {
        Edit edit = new Edit(0, 5, 8, 12);
        DiffEntryWrapper wrapper = Mockito.mock(DiffEntryWrapper.class);
        Mockito.doReturn("/repo/src/main/java/Sample.java").when(wrapper).getAbsoluteNewPath();
        Mockito.doReturn(Collections.singletonList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Collections.singletonList(wrapper));

        RuleViolation violation = Mockito.mock(RuleViolation.class);
        Mockito.doReturn("/repo/src/main/java/Other.java").when(violation).getFilename();
        Mockito.doReturn(10).when(violation).getBeginLine();
        Mockito.doReturn(10).when(violation).getEndLine();

        assertFalse(filter.accept(violation));
    }

}
