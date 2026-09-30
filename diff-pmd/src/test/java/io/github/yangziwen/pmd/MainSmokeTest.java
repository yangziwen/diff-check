package io.github.yangziwen.pmd;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 端到端冒烟测试：走完 文件扫描 → 规则解析 → 违规报告 的完整链路。
 *
 * run() 的返回值语义：0 成功无违规，4 发现违规（failOnViolation 默认开启），1 执行出错。
 */
public class MainSmokeTest {

    @Test
    public void reportViolationsForBadCode() throws Exception {
        File dir = Files.createTempDirectory("diff-pmd-smoke").toFile();
        File sample = new File(dir, "Sample.java");
        Files.write(sample.toPath(), ("public class Sample {\n"
                + "    public void foo() {\n"
                + "        try {\n"
                + "            int i = 1;\n"
                + "        } catch (Exception e) {\n"
                + "        }\n"
                + "    }\n"
                + "}\n").getBytes());

        int status = Main.run(new String[] {
                "-d", dir.getAbsolutePath(),
                "-R", "rulesets/java/empty.xml",
                "-f", "text"});

        assertEquals(4, status, "empty catch block should be reported as violation");
    }

    @Test
    public void noViolationForCleanCode() throws Exception {
        File dir = Files.createTempDirectory("diff-pmd-smoke").toFile();
        File sample = new File(dir, "Sample.java");
        Files.write(sample.toPath(), ("public class Sample {\n"
                + "    public int foo() {\n"
                + "        return 1;\n"
                + "    }\n"
                + "}\n").getBytes());

        int status = Main.run(new String[] {
                "-d", dir.getAbsolutePath(),
                "-R", "rulesets/java/empty.xml",
                "-f", "text"});

        assertEquals(0, status, "clean code should not be reported");
    }

}
