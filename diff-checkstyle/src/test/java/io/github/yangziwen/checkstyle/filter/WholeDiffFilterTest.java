package io.github.yangziwen.checkstyle.filter;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Filter;
import com.puppycrawl.tools.checkstyle.api.Violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WholeDiffFilterTest {

    @Test
    public void acceptEmptyList() {
        Filter filter = new WholeDiffFilter(Collections.emptyList());
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertFalse(result);
    }

    @Test
    public void acceptUnmatched() {
        List<String> checks = Collections.singletonList("something");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertFalse(result);
    }

    @Test
    public void acceptMatchOne() {
        List<String> checks = new ArrayList<>();
        checks.add("something");
        checks.add("java.lang.Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    @Test
    public void acceptSimpleNameMatch() {
        // 简单类名精确相等："Object" 只命中简单类名为 Object 的规则
        List<String> checks = Collections.singletonList("Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    @Test
    public void acceptFqcnMatch() {
        // 支持传全限定名
        List<String> checks = Collections.singletonList("java.lang.Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    @Test
    public void noOverMatchForCommonSubstring() {
        // 子串片段不应误伤同名族规则："Check" 不能命中 FallThroughCheck，
        // "Regexp" 不能命中 RegexpSinglelineCheck（简单类名不相等）
        Filter checkFilter = new WholeDiffFilter(Collections.singletonList("Check"));
        Filter regexpFilter = new WholeDiffFilter(Collections.singletonList("Regexp"));
        AuditEvent fallThrough = buildAuditEventFor(
                com.puppycrawl.tools.checkstyle.checks.coding.FallThroughCheck.class);
        AuditEvent regexpSingleline = buildAuditEventFor(
                com.puppycrawl.tools.checkstyle.checks.regexp.RegexpSinglelineCheck.class);
        assertFalse(checkFilter.accept(fallThrough));
        assertFalse(regexpFilter.accept(regexpSingleline));
    }


    @Test
    public void acceptSingleMatch() {
        List<String> checks = Collections.singletonList("java.lang.Object");
        Filter filter = new WholeDiffFilter(checks);
        AuditEvent event = buildAuditEvent();
        boolean result = filter.accept(event);
        assertTrue(result);
    }

    private static AuditEvent buildAuditEvent() {
        return buildAuditEventFor(Object.class);
    }

    private static AuditEvent buildAuditEventFor(Class<?> sourceClass) {
        Object source = new Object();
        Violation violation = new Violation(0, "", "", null, "", sourceClass, "");
        return new AuditEvent(source, "", violation);
    }
}