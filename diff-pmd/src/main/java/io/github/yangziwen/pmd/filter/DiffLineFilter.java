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
            if (edit.getBeginB() <= violation.getBeginLine() && edit.getEndB() >= violation.getEndLine()) {
                return true;
            }
        }
        return false;
    }

}
