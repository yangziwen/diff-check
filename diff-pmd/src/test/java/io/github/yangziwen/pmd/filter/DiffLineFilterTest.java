package io.github.yangziwen.pmd.filter;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import org.eclipse.jgit.diff.Edit;
import org.junit.Assert;
import org.junit.Test;


import java.io.File;
import java.util.Collections;

public class DiffLineFilterTest {

    public static final String FILE_PATH = "/file";
    private static final String ROOT_PATH = "/";

    @Test
    public void noMatchingEdit() {
        DiffLineFilter diffLineFilter = new DiffLineFilter(Collections.emptyList());
        RuleViolation violation = new DummyRuleViolation(7, 7, "/unmatched/path");
        boolean result = diffLineFilter.accept(violation);
        Assert.assertFalse(result);
    }

    @Test
    public void emptyEditList() {
        DiffEntryWrapper wrapper = DiffEntryWrapper.builder()
                .diffEntry(new DummyDiffEntry(DiffLineFilterTest.FILE_PATH))
                .gitDir(new File(ROOT_PATH))
                .editList(Collections.emptyList()).build();
        DiffLineFilter diffLineFilter = new DiffLineFilter(Collections.singletonList(wrapper));
        RuleViolation violation = new DummyRuleViolation(7, 7, FILE_PATH);
        boolean result = diffLineFilter.accept(violation);
        Assert.assertFalse(result);
    }

    @Test
    public void insertEditWithViolation() {
        Edit insert = new Edit(6, 6, 6, 7);
        DiffEntryWrapper wrapper = buildWrapper(insert);
        DiffLineFilter diffLineFilter = new DiffLineFilter(Collections.singletonList(wrapper));
        RuleViolation violation = new DummyRuleViolation(7, 7, FILE_PATH);
        boolean result = diffLineFilter.accept(violation);
        Assert.assertTrue(result);
    }

    @Test
    public void replaceEditWithOriginalViolation() {
        Edit replace = new Edit(6, 7, 6, 8);
        DiffLineFilter diffLineFilter = new DiffLineFilter(Collections.singletonList(buildWrapper(replace)));
        RuleViolation violationOnOriginalLine = new DummyRuleViolation(replace.getBeginA(), replace.getBeginA(), FILE_PATH);
        boolean result = diffLineFilter.accept(violationOnOriginalLine);
        Assert.assertFalse(result);
    }

    @Test
    public void replaceEditWithNewViolation() {
        Edit replace = new Edit(6, 7, 6, 8);
        DiffLineFilter diffLineFilter = new DiffLineFilter(Collections.singletonList(buildWrapper(replace)));
        int lineWithViolation = replace.getBeginA() + 1;
        RuleViolation violationOnChangedLine = new DummyRuleViolation(lineWithViolation, lineWithViolation, FILE_PATH);
        boolean result = diffLineFilter.accept(violationOnChangedLine);
        Assert.assertTrue(result);
    }

    private static DiffEntryWrapper buildWrapper(Edit edit) {
        return DiffEntryWrapper.builder()
                .diffEntry(new DummyDiffEntry(DiffLineFilterTest.FILE_PATH))
                .gitDir(new File(ROOT_PATH))
                .editList(Collections.singletonList(edit)).build();
    }

    private static class DummyRuleViolation implements RuleViolation {

        private final int beginLine;
        private final int endLine;
        private final String filename;

        DummyRuleViolation(int beginLine, int endLine, String filename) {
            this.beginLine = beginLine;
            this.endLine = endLine;
            this.filename = filename;
        }

        @Override
        public Rule getRule() {
            return null;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public boolean isSuppressed() {
            return false;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public int getBeginLine() {
            return beginLine;
        }

        @Override
        public int getBeginColumn() {
            return 0;
        }

        @Override
        public int getEndLine() {
            return endLine;
        }

        @Override
        public int getEndColumn() {
            return 0;
        }

        @Override
        public String getPackageName() {
            return "";
        }

        @Override
        public String getClassName() {
            return "";
        }

        @Override
        public String getMethodName() {
            return "";
        }

        @Override
        public String getVariableName() {
            return "";
        }
    }

}