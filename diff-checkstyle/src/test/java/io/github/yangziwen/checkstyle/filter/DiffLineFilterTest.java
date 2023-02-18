package io.github.yangziwen.checkstyle.filter;

import java.util.Arrays;

import org.eclipse.jgit.diff.Edit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.checks.whitespace.EmptyLineSeparatorCheck;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AuditEvent.class, Edit.class })
public class DiffLineFilterTest {

    @Test
    public void testAccept() {
        String fileName = "test";
        int lineNum = 10;
        int beginB = 5;
        int endB = 12;

        AuditEvent event = PowerMockito.mock(AuditEvent.class);
        PowerMockito.doReturn(fileName).when(event).getFileName();
        PowerMockito.doReturn(lineNum).when(event).getLine();

        Edit edit = PowerMockito.mock(Edit.class);
        PowerMockito.doReturn(beginB).when(edit).getBeginB();
        PowerMockito.doReturn(endB).when(edit).getEndB();

        DiffEntryWrapper wrapper = PowerMockito.mock(DiffEntryWrapper.class);
        PowerMockito.doReturn(fileName).when(wrapper).getAbsoluteNewPath();
        PowerMockito.doReturn(Arrays.asList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Arrays.asList(wrapper));

        Assert.assertTrue(filter.accept(event));
    }

    @Test
    public void testAcceptWithEmptyLineSeparator() {
        String fileName = "test";
        int lineNum = 10;
        int beginB = 5;
        int endB = 9;

        AuditEvent event = PowerMockito.mock(AuditEvent.class);
        PowerMockito.doReturn(fileName).when(event).getFileName();
        PowerMockito.doReturn(lineNum).when(event).getLine();
        PowerMockito.doReturn(EmptyLineSeparatorCheck.class.getName()).when(event).getSourceName();

        Edit edit = PowerMockito.mock(Edit.class);
        PowerMockito.doReturn(beginB).when(edit).getBeginB();
        PowerMockito.doReturn(endB).when(edit).getEndB();

        DiffEntryWrapper wrapper = PowerMockito.mock(DiffEntryWrapper.class);
        PowerMockito.doReturn(fileName).when(wrapper).getAbsoluteNewPath();
        PowerMockito.doReturn(Arrays.asList(edit)).when(wrapper).getEditList();

        DiffLineFilter filter = new DiffLineFilter(Arrays.asList(wrapper));

        Assert.assertTrue(filter.accept(event));
    }

}
