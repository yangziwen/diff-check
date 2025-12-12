package io.github.yangziwen.jacoco.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

import io.github.yangziwen.jacoco.util.CollectionUtil;
import io.github.yangziwen.jacoco.util.FilterUtil;
import io.github.yangziwen.jacoco.util.LineNumberNodeWrapper;

public class PersonFilter implements IFilter {

    private static final String SOURCE_PATH_PREFIX = "/src/main/java/".replace("/", File.separator);

    private Map<String, BlameResult> classPathBlameResultMap = new HashMap<>();

    private PersonInfo personInfo;

    public PersonFilter(List<MavenProject> projectList, PersonInfo personInfo, List<BlameResult> blameResults) {
        if (CollectionUtil.isEmpty(projectList)) {
            return;
        }
        for (MavenProject  project : projectList) {
            String modulePrefix = generateFullModulePrefix(project);
            for (BlameResult blameResult : blameResults) {
                String path = blameResult.getResultPath();
                if (!path.startsWith(modulePrefix)) {
                    continue;
                }
                String name = StringUtils.replaceOnce(path, modulePrefix, "");
                if (!name.startsWith(File.separator)) {
                    name = File.separator + name;
                }
                if (!name.startsWith(SOURCE_PATH_PREFIX)) {
                    continue;
                }
                name = StringUtils
                        .replaceOnce(name, SOURCE_PATH_PREFIX, "")
                        .replace(File.separator, "/");
                classPathBlameResultMap.put(name, blameResult);
            }
        }
        this.personInfo = personInfo;
    }

    private String generateFullModulePrefix(MavenProject project) {
        List<String> moduleList = new ArrayList<>();
        while (project != null && project.getParent() != null) {
            moduleList.add(project.getBasedir().getName());
            project = project.getParent();
        }
        Collections.reverse(moduleList);
        return StringUtils.join(moduleList.toArray(), File.separator);
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
