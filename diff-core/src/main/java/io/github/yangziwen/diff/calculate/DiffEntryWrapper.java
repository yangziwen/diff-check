package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;

import lombok.Builder;
import lombok.Getter;

/**
 * The diff entry wrapper
 *
 * @author yangziwen
 */
@Getter
@Builder
public class DiffEntryWrapper {

    private File gitDir;

    private DiffEntry diffEntry;

    private List<Edit> editList;

    /**
     * Determines whether the file is deleted
     *
     * @return True if the file is deleted
     */
    public boolean isDeleted() {
        return diffEntry.getChangeType() == ChangeType.DELETE;
    }

    /**
     * Determines whether there is only deleted edits in the file modification
     *
     * @return True if all the edits are deleted type
     */
    public boolean isAllDeletedEdits() {
        return editList.stream().allMatch(edit -> edit.getType() == Type.DELETE);
    }

    public String getNewPath() {
        return diffEntry.getNewPath();
    }

    public String getAbsoluteNewPath() {
        return getNewFile().getAbsolutePath();
    }

    public File getNewFile() {
        return new File(gitDir, diffEntry.getNewPath());
    }

}
