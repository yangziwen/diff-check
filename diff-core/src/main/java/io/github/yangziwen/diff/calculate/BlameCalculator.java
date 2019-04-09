package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import lombok.Builder;
import lombok.Getter;

/**
 * The blame calculator
 * calculate the blame result of the specified files
 *
 * @author yangziwen
 */
@Getter
@Builder
public class BlameCalculator {

    @Builder.Default
    private RawTextComparator comparator = RawTextComparator.DEFAULT;

    public List<BlameResult> calculate(
            File repoDir,
            List<String> filePathList,
            String startRev) throws Exception {
        try (Git git = Git.open(repoDir);
                ObjectReader reader = git.getRepository().newObjectReader();
                RevWalk rw = new RevWalk(git.getRepository())) {
            RevCommit startCommit = rw.parseCommit(git.getRepository().resolve(startRev));
            List<BlameResult> resultList = new ArrayList<>();
            for (String filePath : filePathList) {
                BlameResult result = calculateBlame(filePath, startCommit, git);
                resultList.add(result);
            }
            return resultList;
        }
    }

    private BlameResult calculateBlame(
            String filePath,
            RevCommit startCommit,
            Git git) throws GitAPIException {
        return git.blame().setFilePath(filePath)
                .setStartCommit(startCommit)
                .setTextComparator(comparator)
                .setFollowFileRenames(true)
                .call();
    }

}
