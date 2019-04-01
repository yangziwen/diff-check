package io.github.yangziwen.diff.calculate;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * The merge base calculator
 * calculate the merge base between the specified two refs
 *
 * @author yangziwen
 */
public class MergeBaseCalculator {

    /**
     * calculate the merge base between two refs
     *
     * @param repoDir           the git directory
     * @param ref1              the ref
     * @param ref2              the other ref
     * @return                  the merge base
     * @throws Exception        throw Exception when error happens
     */
    public String calculateMergeBase(File repoDir, String ref1, String ref2) throws Exception {
        try (Git git = Git.open(repoDir);
                ObjectReader reader = git.getRepository().newObjectReader();
                RevWalk rw = new RevWalk(git.getRepository())) {

            RevCommit commit1 = rw.parseCommit(git.getRepository().resolve(ref1));
            RevCommit commit2 = rw.parseCommit(git.getRepository().resolve(ref2));

            rw.setRevFilter(RevFilter.MERGE_BASE);
            rw.markStart(commit1);
            rw.markStart(commit2);

            RevCommit mergeBase = rw.next();

            return mergeBase != null ? mergeBase.name() : "";

        }
    }

}
