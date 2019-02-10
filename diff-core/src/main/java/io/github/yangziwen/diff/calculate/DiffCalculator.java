package io.github.yangziwen.diff.calculate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import lombok.Builder;
import lombok.Getter;

/**
 * The diff calculator
 * calculate the diff entries and the corresponding edits between the sepcifed 2 revisions
 *
 * @author yangziwen
 */
@Getter
@Builder
public class DiffCalculator {

    private DiffAlgorithm diffAlgorithm;

    @Builder.Default
    private RawTextComparator comparator = RawTextComparator.DEFAULT;

    @Builder.Default
    private int bigFileThreshold = DiffHelper.DEFAULT_BIG_FILE_THRESHOLD;

    /**
     * calculate the diff between the old revision and the new revision
     *
     * @param repoDir               the git directory
     * @param oldRev                the old revision
     * @param newRev                the new revision
     * @param includeStagedCodes    include the staged codes
     * @return
     * @throws Exception            throw Exception when error happens
     */
    public List<DiffEntryWrapper> calculateDiff(
            File repoDir,
            String oldRev,
            String newRev,
            boolean includeStagedCodes) throws Exception {

        try (Git git = Git.open(repoDir);
                ObjectReader reader = git.getRepository().newObjectReader();
                RevWalk rw = new RevWalk(git.getRepository())) {

            RevCommit oldCommit = rw.parseCommit(git.getRepository().resolve(oldRev));
            RevCommit newCommit = rw.parseCommit(git.getRepository().resolve(newRev));

            List<DiffEntryWrapper> wrappers = new ArrayList<>();

            if (includeStagedCodes) {
                wrappers.addAll(doCalculateIndexedDiff(oldCommit, reader, git, repoDir));
            }

            Set<String> indexedPathSet = wrappers.stream()
                    .map(wrapper -> wrapper.getNewPath())
                    .collect(Collectors.toSet());

            wrappers.addAll(doCalculateCommitDiff(oldCommit, newCommit, reader, git, repoDir, indexedPathSet));

            return wrappers;
        }
    }

    private List<DiffEntryWrapper> doCalculateCommitDiff(
            RevCommit oldCommit,
            RevCommit newCommit,
            ObjectReader reader,
            Git git,
            File repoDir,
            Set<String> excludedPathSet) throws Exception {

        if (Objects.equals(oldCommit.getId(), newCommit.getId())) {
            return Collections.emptyList();
        }

        if (Objects.equals(oldCommit.getTree().getId(), newCommit.getTree().getId())) {
            return Collections.emptyList();
        }

        RenameDetector detector = new RenameDetector(git.getRepository());
        AbstractTreeIterator oldTree = new CanonicalTreeParser(null, reader, oldCommit.getTree());
        AbstractTreeIterator newTree = new CanonicalTreeParser(null, reader, newCommit.getTree());

        List<DiffEntry> entries = git.diff()
                .setOldTree(oldTree)
                .setNewTree(newTree)
                .call();
        detector.reset();
        detector.addAll(entries);
        entries = detector.compute();

        return entries.stream()
                .filter(entry -> !excludedPathSet.contains(entry.getNewPath()))
                .map(entry -> {
                    RawText oldText = newRawText(entry, DiffEntry.Side.OLD, reader);
                    RawText newText = newRawText(entry, DiffEntry.Side.NEW, reader);
                    return DiffEntryWrapper.builder()
                            .gitDir(repoDir)
                            .diffEntry(entry)
                            .editList(calculateEditList(oldText, newText))
                            .build();
                }).collect(Collectors.toList());
    }

    private List<DiffEntryWrapper> doCalculateIndexedDiff(
            RevCommit oldCommit,
            ObjectReader reader,
            Git git,
            File repoDir) throws Exception {
        Set<String> indexedPathSet = new HashSet<>();
        Status status = git.status().call();
        indexedPathSet.addAll(status.getAdded());
        indexedPathSet.addAll(status.getChanged());
        Map<String, BlobWrapper> indexedFileContentMap = getIndexedFileContentMap(git, indexedPathSet);
        Map<String, BlobWrapper> oldRevFileContentMap = getRevFileContentMap(git, oldCommit, indexedPathSet, reader);
        return indexedPathSet.stream()
                .map(filePath -> {
                    BlobWrapper oldBlob = oldRevFileContentMap.get(filePath);
                    RawText oldText = oldBlob != null ? new RawText(oldBlob.getContent()) : RawText.EMPTY_TEXT;
                    RawText newText = new RawText(indexedFileContentMap.get(filePath).getContent());
                    DiffEntry entry = oldBlob == null
                            ? DiffHelper.createAddDiffEntry(filePath, oldCommit)
                            : DiffHelper.createModifyDiffEntry(filePath);
                    return DiffEntryWrapper.builder()
                            .gitDir(repoDir)
                            .diffEntry(entry)
                            .editList(calculateEditList(oldText, newText))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, BlobWrapper> getRevFileContentMap(
            Git git, RevCommit commit, Set<String> filePathSet, ObjectReader reader) throws Exception {
        if (filePathSet == null || filePathSet.isEmpty()) {
            return Collections.emptyMap();
        }
        TreeFilter filter = filePathSet.size() > 1
                ? OrTreeFilter.create(filePathSet.stream()
                        .map(PathFilter::create)
                        .collect(Collectors.toList()))
                : PathFilter.create(filePathSet.iterator().next());
         return getContentMapByTreeAndFilter(git, new CanonicalTreeParser(null, reader, commit.getTree()), filter);
    }

    private Map<String, BlobWrapper> getIndexedFileContentMap(Git git, Set<String> filePathSet) throws Exception {
        if (filePathSet == null || filePathSet.isEmpty()) {
            return Collections.emptyMap();
        }
        DirCache index = git.getRepository().readDirCache();
        TreeFilter filter = filePathSet.size() > 1
                ? OrTreeFilter.create(filePathSet.stream()
                        .map(PathFilter::create)
                        .collect(Collectors.toList()))
                : PathFilter.create(filePathSet.iterator().next());
        return getContentMapByTreeAndFilter(git, new DirCacheIterator(index), filter);
    }

    private Map<String, BlobWrapper> getContentMapByTreeAndFilter(
            Git git, AbstractTreeIterator tree, TreeFilter filter) throws Exception {
        Map<String, BlobWrapper> contentMap = new LinkedHashMap<>();
        try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(filter);
            while (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = git.getRepository().open(objectId);
                BlobWrapper blobWrapper = BlobWrapper.builder()
                        .blobId(objectId)
                        .content(loader.getBytes())
                        .build();
                contentMap.put(treeWalk.getPathString(), blobWrapper);
            }
        }
        return contentMap;
    }

    private RawText newRawText(DiffEntry entry, DiffEntry.Side side, ObjectReader reader) {
        try {
            return new RawText(DiffHelper.open(entry, side, reader, bigFileThreshold));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Edit> calculateEditList(RawText oldText, RawText newText) {
        EditList edits = diffAlgorithm.diff(comparator, oldText, newText);
        List<Edit> editList = new ArrayList<Edit>();
        for (Edit edit : edits) {
            editList.add(edit);
        }
        return editList;
    }

}
