package io.github.yangziwen.diff.calculate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import org.eclipse.jgit.diff.ContentSource;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;

/**
 * The diff helper
 * Provide some useful methods to help finish diff calculation
 *
 * @author yangziwen
 */
public class DiffHelper {

    public static final int DEFAULT_BIG_FILE_THRESHOLD = 10 * 1024 * 1024;

    private static final Field DIFF_ENTRY_OLD_ID_FIELD = getField(DiffEntry.class, "oldId");

    private static final Field DIFF_ENTRY_NEW_ID_FIELD = getField(DiffEntry.class, "newId");

    private static final Method DIFF_ENTRY_ADD_METHOD = getDiffEntryAddMethod();

    private static final Method DIFF_ENTRY_MODIFY_METHOD = getDiffEntryModifyMethod();

    private static final byte[] EMPTY = new byte[] {};

    private static final byte[] BINARY = new byte[] {};

    private DiffHelper() {}

    /**
     * read content from the diff entry
     *
     * @param entry
     * @param side
     * @param reader
     * @param bigFileThreshold
     * @return
     * @throws Exception
     */
    public static byte[] open(
            DiffEntry entry,
            DiffEntry.Side side,
            ObjectReader reader,
            int bigFileThreshold) throws Exception {
        if (entry.getMode(side) == FileMode.GITLINK) {
            return writeGitLinkText(entry.getId(side));
        }
        if (entry.getMode(side) == FileMode.MISSING) {
            return EMPTY;
        }
        if (entry.getMode(side).getObjectType() != Constants.OBJ_BLOB) {
            return EMPTY;
        }
        AbbreviatedObjectId id = entry.getId(side);
        if (!id.isComplete()) {
            Collection<ObjectId> ids = reader.resolve(id);
            if (ids.size() == 1) {
                id = AbbreviatedObjectId.fromObjectId(ids.iterator().next());
                switch (side) {
                    case OLD:
                        DIFF_ENTRY_OLD_ID_FIELD.set(entry, id);
                        break;
                    case NEW:
                        DIFF_ENTRY_NEW_ID_FIELD.set(entry, id);
                        break;
                    default:
                        break;
                }
            }
            else if (ids.size() == 0) {
                throw new MissingObjectException(id, Constants.OBJ_BLOB);
            }
            else {
                throw new AmbiguousObjectException(id, ids);
            }
        }
        ContentSource cs = ContentSource.create(reader);
        try {
            ObjectLoader ldr = new ContentSource.Pair(cs, cs).open(side, entry);
            return ldr.getBytes(bigFileThreshold);

        } catch (LargeObjectException.ExceedsLimit overLimit) {
            return BINARY;

        } catch (LargeObjectException.ExceedsByteArrayLimit overLimit) {
            return BINARY;

        } catch (LargeObjectException.OutOfMemory tooBig) {
            return BINARY;

        } catch (LargeObjectException tooBig) {
            tooBig.setObjectId(id.toObjectId());
            throw tooBig;
        }
    }

    private static byte[] writeGitLinkText(AbbreviatedObjectId id) {
        if (ObjectId.zeroId().equals(id.toObjectId())) {
            return EMPTY;
        }
        return Constants.encodeASCII("Subproject commit " + id.name() + "\n");
    }

    private static <T> Field getField(Class<T> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DiffEntry createAddDiffEntry(String path, AnyObjectId id) {
        try {
            return (DiffEntry) DIFF_ENTRY_ADD_METHOD.invoke(null, path, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DiffEntry createModifyDiffEntry(String path) {
        try {
            return (DiffEntry) DIFF_ENTRY_MODIFY_METHOD.invoke(null, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Method getDiffEntryAddMethod() {
        Method method;
        try {
            method = DiffEntry.class.getDeclaredMethod("add", String.class, AnyObjectId.class);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Method getDiffEntryModifyMethod() {
        Method method;
        try {
            method = DiffEntry.class.getDeclaredMethod("modify", String.class);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
