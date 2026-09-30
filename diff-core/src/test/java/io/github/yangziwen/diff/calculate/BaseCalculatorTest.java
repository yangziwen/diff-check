package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试基类，提供隔离的临时 git 工作仓库。
 *
 * 替代 jgit junit 的 RepositoryTestCase（JUnit 4 生命周期），
 * 改用 JUnit 5 生命周期，并通过 MockSystemReader 隔离本机 git 全局配置，
 * 避免用户配置（如 commit.gpgsign）影响测试。
 *
 * @author yangziwen
 */
public abstract class BaseCalculatorTest {

    protected FileRepository db;

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        SystemReader.setInstance(new MockSystemReader());
        tempDir = Files.createTempDirectory("diff-check-test").toFile();
        File workTree = new File(tempDir, "repo");
        try (Git ignored = Git.init().setDirectory(workTree).call()) {
            // 仅初始化仓库，仓库句柄单独打开，避免 init 的 Git 关闭时连带关闭 db
        }
        db = new FileRepository(new File(workTree, Constants.DOT_GIT));
    }

    @AfterEach
    void tearDown() throws Exception {
        SystemReader.setInstance(null);
        if (db != null) {
            db.close();
        }
        if (tempDir != null) {
            recursiveDelete(tempDir);
        }
    }

    private void recursiveDelete(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                recursiveDelete(child);
            }
        }
        file.delete();
    }

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
