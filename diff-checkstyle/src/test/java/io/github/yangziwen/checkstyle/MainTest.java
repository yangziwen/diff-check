package io.github.yangziwen.checkstyle;

import java.io.File;
import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {

    @Test
    public void testRelativeExcludeDirExcludesSubPath() {
        File repoDir = new File("/repo");
        assertTrue(Main.isDiffPathExcluded(
                "src/generated/Foo.java", repoDir,
                Collections.singletonList(new File("src/generated")),
                Collections.emptyList()));
    }

    @Test
    public void testAbsoluteExcludeDirExcludesSubPath() {
        File repoDir = new File("/repo");
        assertTrue(Main.isDiffPathExcluded(
                "src/generated/Foo.java", repoDir,
                Collections.singletonList(new File("/repo/src/generated")),
                Collections.emptyList()));
    }

    @Test
    public void testExcludeExactFilePath() {
        File repoDir = new File("/repo");
        assertTrue(Main.isDiffPathExcluded(
                "src/main/java/Bar.java", repoDir,
                Collections.singletonList(new File("src/main/java/Bar.java")),
                Collections.emptyList()));
    }

    @Test
    public void testNonExcludedPathIsKept() {
        File repoDir = new File("/repo");
        assertFalse(Main.isDiffPathExcluded(
                "src/main/java/Bar.java", repoDir,
                Collections.singletonList(new File("src/generated")),
                Collections.emptyList()));
    }

    @Test
    public void testPrefixPathIsNotOverExcluded() {
        // 排除 src/gen 时不应误伤 src/generated
        File repoDir = new File("/repo");
        assertFalse(Main.isDiffPathExcluded(
                "src/generated/Foo.java", repoDir,
                Collections.singletonList(new File("src/gen")),
                Collections.emptyList()));
    }

    @Test
    public void testExcludeRegex() {
        File repoDir = new File("/repo");
        assertTrue(Main.isDiffPathExcluded(
                "src/generated/Foo.java", repoDir,
                Collections.emptyList(),
                Collections.singletonList(Pattern.compile("src/generated/.*"))));
    }

}
