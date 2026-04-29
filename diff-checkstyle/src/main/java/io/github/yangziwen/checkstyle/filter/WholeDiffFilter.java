package io.github.yangziwen.checkstyle.filter;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Filter;
import com.puppycrawl.tools.checkstyle.api.Violation;

import java.util.List;

public class WholeDiffFilter implements Filter {

    private final List<String> checkNames;

    public WholeDiffFilter(List<String> checks) {
        this.checkNames = checks;
    }

    @Override
    public boolean accept(AuditEvent event) {
        Violation v = event.getViolation();
        return checkNames.stream().anyMatch(checkName -> v.getSourceName().contains(checkName));
    }
}
