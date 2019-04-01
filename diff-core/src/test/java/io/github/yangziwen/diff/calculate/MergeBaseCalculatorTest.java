package io.github.yangziwen.diff.calculate;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MergeBaseCalculatorTest extends BaseCalculatorTest {

    private static final Person DEFAULT_USER = Person.builder()
            .name("test")
            .email("test@test.com")
            .build();

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        setUp();
    }

    @Test
    public void testCalculateMergeBase() throws Exception {

        try (Git git = new Git(db);
                ObjectReader reader  = git.getRepository().newObjectReader()) {

            File repoDir = git.getRepository().getDirectory().getParentFile();

            File file = new File(repoDir, "file.txt");
            writeStringToFile(file, "hello\n");
            git.add()
                .addFilepattern(file.getName())
                .call();
            RevCommit baseCommit = doCommit(git);

            writeStringToFile(file, "hello world\n");
            git.add()
                .addFilepattern(file.getName())
                .call();
            RevCommit commit1 = doCommit(git);

            git.reset().setMode(ResetType.HARD).setRef(baseCommit.name()).call();

            writeStringToFile(file, "hello code\n");
            git.add()
                .addFilepattern(file.getName())
                .call();
            RevCommit commit2 = doCommit(git);

            String mergeBase = new MergeBaseCalculator()
                .calculateMergeBase(repoDir, commit1.name(), commit2.name());

            Assert.assertEquals(baseCommit.name(), mergeBase);
        }

    }

    private RevCommit doCommit(Git git) throws Exception {
        return super.doCommit(git, DEFAULT_USER, DEFAULT_USER, "new commit");
    }

}
