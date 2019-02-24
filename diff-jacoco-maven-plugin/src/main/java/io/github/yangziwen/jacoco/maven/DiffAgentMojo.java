package io.github.yangziwen.jacoco.maven;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.maven.AgentMojo;

import io.github.yangziwen.diff.calculate.DiffCalculator;
import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import io.github.yangziwen.jacoco.filter.DiffFilter;
import io.github.yangziwen.jacoco.util.FilterUtil;

@Mojo(
        name = "prepare-agent",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class DiffAgentMojo extends AgentMojo {

    @Parameter(property = "jacoco.diff.oldrev", defaultValue = "")
    private String oldRev;

    @Parameter(property = "jacoco.diff.newrev", defaultValue = "")
    private String newRev;

    @Override
    public void executeMojo() {
        try {
            if (StringUtils.isNotBlank(oldRev) && StringUtils.isNotBlank(newRev)) {
                injectDiffFilter(oldRev, newRev);
            }
        } catch (Exception e) {
            getLog().error("failed to inject diff filter for old rev [" + oldRev + "] and new rev [" + newRev + "]");
        }
        super.executeMojo();
    }

    private void injectDiffFilter(String oldRev, String newRev) throws Exception {

        File baseDir = getProject().getBasedir();

        File gitDir = new FileRepositoryBuilder().findGitDir(baseDir).getGitDir();

        gitDir = new File(gitDir.getAbsolutePath().replaceAll("\\.git$", ""));

        DiffCalculator calculator = DiffCalculator.builder()
                .diffAlgorithm(new HistogramDiff())
                .build();
        List<DiffEntryWrapper> diffEntryList = calculator.calculateDiff(gitDir, oldRev, newRev, false)
                .stream()
                .filter(diffEntry -> !diffEntry.isDeleted())
                .collect(Collectors.toList());
        IFilter diffFilter = new DiffFilter(baseDir, gitDir, diffEntryList);
        FilterUtil.appendFilter(diffFilter);
    }

}
