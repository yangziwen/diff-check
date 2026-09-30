package io.github.yangziwen.checkstyle.filter;

import java.util.Arrays;

import org.eclipse.jgit.diff.Edit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.checks.whitespace.EmptyLineSeparatorCheck;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiffLineFilterTest {

    @Test
    public void testAccept() {
        String fileName = "test";
        int lineNum = 10;
        int beginB = 5;
        int endB = 12;

        AuditEvent event = Mockito.mock(AuditEvent.class);
        Mockito.doReturn(fileName).when(event).getFileName();
        Mockito.doReturn(lineNum).when(event).getLine();

        Edit edit = Mockito.mock(Edit.class);
        Mockito.doReturn(beginB).when(edit).getBeginB();
        Mockito.doReturn(endB).when(edit).getEndB();

        DiffEntryWrapper wrapper = Mockito.mock(DiffEntryWrapper.class);
        Mockito.doReturn(fileName).when(wrapper).getAbsoluteNewPath();
        Mockito.doReturn(Arrays.asList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Arrays.asList(wrapper));

        assertTrue(filter.accept(event));
    }

    @Test
    public void testAcceptWithEmptyLineSeparator() {
        String fileName = "test";
        int lineNum = 10;
        int beginB = 5;
        int endB = 9;

        AuditEvent event = Mockito.mock(AuditEvent.class);
        Mockito.doReturn(fileName).when(event).getFileName();
        Mockito.doReturn(lineNum).when(event).getLine();
        Mockito.doReturn(EmptyLineSeparatorCheck.class.getName()).when(event).getSourceName();

        Edit edit = Mockito.mock(Edit.class);
        Mockito.doReturn(beginB).when(edit).getBeginB();
        Mockito.doReturn(endB).when(edit).getEndB();

        DiffEntryWrapper wrapper = Mockito.mock(DiffEntryWrapper.class);
        Mockito.doReturn(fileName).when(wrapper).getAbsoluteNewPath();
        Mockito.doReturn(Arrays.asList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Arrays.asList(wrapper));

        assertTrue(filter.accept(event));
    }

}
