package io.github.yangziwen.pmd.filter;

import org.eclipse.jgit.diff.DiffEntry;

public class DummyDiffEntry extends DiffEntry {

    private final String filePath;

    public DummyDiffEntry(String filePath) {
        super();
        this.filePath = filePath;
    }

    @Override
    public String getNewPath() {
        return filePath;
    }
}