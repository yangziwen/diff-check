package io.github.yangziwen.jacoco.maven;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.maven.AgentMojo;

import io.github.yangziwen.diff.calculate.BlameCalculator;
import io.github.yangziwen.diff.calculate.DiffCalculator;
import io.github.yangziwen.diff.calculate.DiffEntryWrapper;
import io.github.yangziwen.diff.calculate.MergeBaseCalculator;
import io.github.yangziwen.jacoco.filter.DiffFilter;
import io.github.yangziwen.jacoco.filter.PersonFilter;
import io.github.yangziwen.jacoco.filter.PersonFilter.PersonInfo;
import io.github.yangziwen.jacoco.filter.PersonFilter.PersonType;
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

    @Parameter(property = "jacoco.author.name", defaultValue = "")
    private String authorName;

    @Parameter(property = "jacoco.author.email", defaultValue = "")
    private String authorEmail;

    @Parameter(property = "jacoco.committer.name", defaultValue = "")
    private String committerName;

    @Parameter(property = "jacoco.committer.email", defaultValue = "")
    private String committerEmail;

    @Override
    public void executeMojo() {
        try {
            if ((StringUtils.isNotBlank(oldRev) && StringUtils.isNotBlank(newRev))
                    || StringUtils.isNotBlank(againstRef)) {
                injectDiffFilter();
            }
        } catch (Exception e) {
            getLog().error("failed to inject diff filter for old rev [" + oldRev + "] and new rev [" + newRev + "]");
        }
        super.executeMojo();
    }

    private void injectDiffFilter() throws Exception {

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

        if (CollectionUtils.isEmpty(diffEntryList)) {
            return;
        }

        if (needAuthorFilter() || needCommitterFilter()) {

            List<String> filePathList = diffEntryList.stream()
                    .map(DiffEntryWrapper::getNewPath)
                    .collect(Collectors.toList());

            List<BlameResult> blameResults = BlameCalculator.builder().build()
                    .calculate(gitDir, filePathList, newRev);

            if (needAuthorFilter()) {
                PersonInfo author = new PersonInfo(authorName, authorEmail, PersonType.AUTHOR);
                IFilter authorFilter = new PersonFilter(getProject(), gitDir, author, blameResults);
                FilterUtil.appendFilter(authorFilter);
            }

            if (needCommitterFilter()) {
                PersonInfo committer = new PersonInfo(committerName, committerEmail, PersonType.COMMITTER);
                IFilter committerFilter = new PersonFilter(getProject(), gitDir, committer, blameResults);
                FilterUtil.appendFilter(committerFilter);
            }

        }

    }

    private String calculateMergeBase(File gitDir, String ref1, String ref2) throws Exception {

        try {
            return new MergeBaseCalculator().calculateMergeBase(gitDir, ref1, ref2);
        } catch (Exception e) {
            getLog().error("failed to find the merge base between [" + ref1 + "] and [" + ref2 + "]");
            throw e;
        }

    }

    private boolean needAuthorFilter() {
        return StringUtils.isNotBlank(authorName) || StringUtils.isNotBlank(authorEmail);
    }

    private boolean needCommitterFilter() {
        return StringUtils.isNotBlank(committerName) || StringUtils.isNotBlank(committerEmail);
    }

}
