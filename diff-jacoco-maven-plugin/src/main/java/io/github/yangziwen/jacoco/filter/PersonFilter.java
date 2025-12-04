package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.PersonIdent;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import io.github.yangziwen.jacoco.util.FilterUtil;
import io.github.yangziwen.jacoco.util.LineNumberNodeWrapper;

public class PersonFilter implements IFilter {

    private static final String SOURCE_PATH_PREFIX = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;

    private Map<String, BlameResult> classPathBlameResultMap = new HashMap<>();

    private PersonInfo personInfo;

    public PersonFilter(MavenProject project, File gitDir, PersonInfo personInfo, List<BlameResult> blameResults) {
        File baseDir = project.getBasedir();
        String baseDirPath = baseDir.getAbsolutePath();
        
        for (BlameResult blameResult : blameResults) {
            String absolutePath = new File(baseDir, blameResult.getResultPath()).getAbsolutePath();
            if (!absolutePath.startsWith(baseDirPath)) {
                continue;
            }
            
            // 去掉项目根目录前缀
            String name = StringUtils.replaceOnce(absolutePath, baseDirPath, "");
            
            // 直接查找 /src/main/java/ 的位置，支持任意深度的嵌套模块
            // 这种方式比逐级去除路径段更高效，且逻辑更清晰
            int srcIndex = name.indexOf(SOURCE_PATH_PREFIX);
            if (srcIndex < 0) {
                // 不是 Java 源文件，跳过
                continue;
            }
            
            // 截取从 /src/main/java/ 之后的部分，得到类的相对路径
            name = name.substring(srcIndex + SOURCE_PATH_PREFIX.length());
            
            // 转换为 JVM 内部格式（使用 / 作为分隔符）
            name = name.replace(File.separator, "/");
            
            // name 与 FilterUtil.getClassPath 一致才能获取到
            classPathBlameResultMap.put(name, blameResult);
        }
        this.personInfo = personInfo;
    }

    @Override
    public void filter(
            MethodNode methodNode,
            IFilterContext context,
            IFilterOutput output) {

        if (personInfo == null) {
            return;
        }

        String classPath = FilterUtil.getClassPath(context);

        InsnList instructions = methodNode.instructions;

        BlameResult blameResult = classPathBlameResultMap.get(classPath);

        if (blameResult == null) {
            output.ignore(instructions.getFirst(), instructions.getLast());
            return;
        }

        List<LineNumberNodeWrapper> nodeWrapperList = FilterUtil.collectLineNumberNodeList(instructions);

        for (LineNumberNodeWrapper nodeWrapper : nodeWrapperList) {
            int line = nodeWrapper.getLine() - 1;
            PersonIdent person = personInfo.getType().getPerson(blameResult, line);
            if (personInfo.accept(person)) {
                nodeWrapper.setIgnored(false);
            }
        }

        for (LineNumberNodeWrapper nodeWrapper : nodeWrapperList) {
            if (nodeWrapper.isIgnored()) {
                output.ignore(nodeWrapper.getNode(), nodeWrapper.getNext());
            }
        }

    }

    public static class PersonInfo {

        private String name;

        private String email;

        private PersonType type;

        public PersonInfo(String name, String email, PersonType type) {
            this.name = name;
            this.email = email;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public PersonType getType() {
            return type;
        }

        public boolean accept(PersonIdent person) {
            if (person == null) {
                return false;
            }
            if (StringUtils.isNotBlank(name) && !name.equals(person.getName())) {
                return false;
            }
            if (StringUtils.isNotBlank(email) && !email.equals(person.getEmailAddress())) {
                return false;
            }
            return true;
        }

    }

    public static enum PersonType {

        AUTHOR {
            @Override
            public PersonIdent getPerson(BlameResult blameResult, int line) {
                return blameResult.getSourceAuthor(line);
            }
        },

        COMMITTER {
            @Override
            public PersonIdent getPerson(BlameResult blameResult, int line) {
                return blameResult.getSourceCommitter(line);
            }
        };

        public abstract PersonIdent getPerson(BlameResult blameResult, int line);

    }

}
