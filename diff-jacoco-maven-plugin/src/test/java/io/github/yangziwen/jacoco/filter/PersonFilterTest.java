package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.yangziwen.jacoco.filter.PersonFilter.PersonInfo;
import io.github.yangziwen.jacoco.filter.PersonFilter.PersonType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonFilterTest {

    @Test
    public void mapBlameResultByModulePrefix() throws Exception {
        MavenProject mid = Mockito.mock(MavenProject.class);
        Mockito.doReturn(new File("/repo/mid")).when(mid).getBasedir();
        Mockito.doReturn(null).when(mid).getParent();

        MavenProject child = Mockito.mock(MavenProject.class);
        Mockito.doReturn(new File("/repo/mid/child")).when(child).getBasedir();
        Mockito.doReturn(mid).when(child).getParent();

        BlameResult blameResult = Mockito.mock(BlameResult.class);
        Mockito.doReturn("child/src/main/java/com/example/Foo.java").when(blameResult).getResultPath();

        PersonFilter filter = new PersonFilter(
                Collections.singletonList(child),
                new PersonInfo("alice", "alice@test.com", PersonType.AUTHOR),
                Collections.singletonList(blameResult));

        Map<String, ?> map = readClassPathMap(filter);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("com/example/Foo.java"));
    }

    @Test
    public void skipBlameResultOutOfModulePrefix() throws Exception {
        MavenProject child = Mockito.mock(MavenProject.class);
        Mockito.doReturn(new File("/repo/mid/child")).when(child).getBasedir();
        MavenProject mid = Mockito.mock(MavenProject.class);
        Mockito.doReturn(new File("/repo/mid")).when(mid).getBasedir();
        Mockito.doReturn(null).when(mid).getParent();
        Mockito.doReturn(mid).when(child).getParent();

        BlameResult blameResult = Mockito.mock(BlameResult.class);
        Mockito.doReturn("other/module/src/main/java/com/example/Bar.java").when(blameResult).getResultPath();

        PersonFilter filter = new PersonFilter(
                Collections.singletonList(child),
                new PersonInfo("alice", "alice@test.com", PersonType.AUTHOR),
                Collections.singletonList(blameResult));

        assertTrue(readClassPathMap(filter).isEmpty());
    }

    @Test
    public void singleModuleProjectUsesEmptyPrefix() throws Exception {
        MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(new File("/repo")).when(project).getBasedir();
        Mockito.doReturn(null).when(project).getParent();

        BlameResult blameResult = Mockito.mock(BlameResult.class);
        Mockito.doReturn("src/main/java/com/example/Foo.java").when(blameResult).getResultPath();

        PersonFilter filter = new PersonFilter(
                Collections.singletonList(project),
                new PersonInfo("alice", "alice@test.com", PersonType.AUTHOR),
                Collections.singletonList(blameResult));

        assertTrue(readClassPathMap(filter).containsKey("com/example/Foo.java"));
    }

    @Test
    public void personInfoAccept() {
        PersonInfo info = new PersonInfo("alice", "alice@test.com", PersonType.AUTHOR);

        assertFalse(info.accept(null));
        assertFalse(info.accept(new PersonIdent("bob", "bob@test.com")));
        assertFalse(info.accept(new PersonIdent("alice", "bob@test.com")));
        assertTrue(info.accept(new PersonIdent("alice", "alice@test.com")));
    }

    @Test
    public void personTypeResolvesCorrectPerson() {
        BlameResult blameResult = Mockito.mock(BlameResult.class);
        PersonIdent author = new PersonIdent("alice", "alice@test.com");
        PersonIdent committer = new PersonIdent("bob", "bob@test.com");
        Mockito.doReturn(author).when(blameResult).getSourceAuthor(5);
        Mockito.doReturn(committer).when(blameResult).getSourceCommitter(5);

        assertEquals(author, PersonType.AUTHOR.getPerson(blameResult, 5));
        assertEquals(committer, PersonType.COMMITTER.getPerson(blameResult, 5));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readClassPathMap(PersonFilter filter) throws Exception {
        Field field = PersonFilter.class.getDeclaredField("classPathBlameResultMap");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(filter);
    }

}
