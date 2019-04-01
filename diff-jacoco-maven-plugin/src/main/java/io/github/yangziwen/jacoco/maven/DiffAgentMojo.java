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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import io.github.yangziwen.diff.calculate.DiffCalculator;
import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import io.github.yangziwen.diff.calculate.MergeBaseCalculator;
import io.github.yangziwen.jacoco.filter.DiffFilter;
import io.github.yangziwen.jacoco.util.FilterUtil;

@Mojo(
        name = "prepare-agent",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class DiffAgentMojo extends AgentMojo {

    private static final AtomicBoolean DIFF_FILTER_INJECTED = new AtomicBoolean(false);

    private static final String REF_HEAD = "HEAD";

    @Parameter(property = "jacoco.diff.oldrev", defaultValue = "")
    private String oldRev;

    @Parameter(property = "jacoco.diff.newrev", defaultValue = "")
    private String newRev;

    @Parameter(property = "jacoco.diff.against", defaultValue = "")
    private String againstRef;

    @Override
    public void executeMojo() {
        try {
            if ((StringUtils.isNotBlank(oldRev) && StringUtils.isNotBlank(newRev))
                    || StringUtils.isNotBlank(againstRef)) {
                injectDiffFilter(oldRev, newRev);
            }
        } catch (Exception e) {
            getLog().error("failed to inject diff filter for old rev [" + oldRev + "] and new rev [" + newRev + "]");
        }
        super.executeMojo();
    }

    private void injectDiffFilter(String oldRev, String newRev) throws Exception {

        if (DIFF_FILTER_INJECTED.getAndSet(true)) {
            return;
        }

        File baseDir = getProject().getBasedir();

        File gitDir = new FileRepositoryBuilder().findGitDir(baseDir).getGitDir();

        gitDir = new File(gitDir.getAbsolutePath().replaceAll("\\.git$", ""));

        if (StringUtils.isNotBlank(againstRef)) {
            oldRev = calculateMergeBase(gitDir, againstRef, REF_HEAD);
            newRev = REF_HEAD;
        }

        DiffCalculator calculator = DiffCalculator.builder()
                .diffAlgorithm(new HistogramDiff())
                .build();
        List<DiffEntryWrapper> diffEntryList = calculator.calculateDiff(gitDir, oldRev, newRev, false)
                .stream()
                .filter(diffEntry -> !diffEntry.isDeleted())
                .collect(Collectors.toList());

        IFilter diffFilter = new DiffFilter(getProject(), gitDir, diffEntryList);

        FilterUtil.appendFilter(diffFilter);
    }

    private String calculateMergeBase(File gitDir, String ref1, String ref2) throws Exception {

        try {
            return new MergeBaseCalculator().calculateMergeBase(gitDir, ref1, ref2);
        } catch (Exception e) {
            getLog().error("failed to find the merge base between [" + ref1 + "] and [" + ref2 + "]");
            throw e;
        }

    }

}
