package io.github.yangziwen.pmd.filter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jgit.diff.Edit;

import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import net.sourceforge.pmd.RuleViolation;

public class DiffLineFilter {

    private final ConcurrentMap<String, List<Edit>> fileEditMap = new ConcurrentHashMap<>();

    public DiffLineFilter(List<DiffEntryWrapper> diffEntryList) {
        for (DiffEntryWrapper diffEntry : diffEntryList) {
            fileEditMap.put(diffEntry.getAbsoluteNewPath(), diffEntry.getEditList());
        }
    }

    public boolean accept(RuleViolation violation) {
        List<Edit> editList = fileEditMap.get(violation.getFilename());
        if (editList == null || editList.isEmpty()) {
            return false;
        }
        for (Edit edit : editList) {
            int offset = insertedWithinExistingLines(edit) ? 1 : 0;
            int changedLineBegin = edit.getBeginB() + offset;
            int changedLineEnd = edit.getEndB() + offset;
            return violationWithinEdit(violation, changedLineBegin, changedLineEnd);
        }
        return false;
    }

    private static boolean insertedWithinExistingLines(Edit edit) {
        return edit.getBeginA() + 1 == edit.getEndA();
    }

    private static boolean violationWithinEdit(RuleViolation violation, int changedLineBegin, int changedLineEnd) {
        return changedLineBegin <= violation.getBeginLine() && changedLineEnd >= violation.getEndLine();
    }

}
