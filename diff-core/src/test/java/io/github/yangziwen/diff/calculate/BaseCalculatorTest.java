package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Ignore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BaseCalculatorTest
 *
 * @author yangziwen
 */
@Ignore
public class BaseCalculatorTest extends RepositoryTestCase {

    protected void writeStringToFile(File file, String content) {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected RevCommit doCommit(Git git, Person author, Person committer, String message) throws Exception {
        return git.commit()
                .setAll(true)
                .setAuthor(author.getName(), author.getEmail())
                .setCommitter(committer.getName(), committer.getEmail())
                .setMessage(message)
                .call();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class Person {

        private String name;

        private String email;

    }

}
