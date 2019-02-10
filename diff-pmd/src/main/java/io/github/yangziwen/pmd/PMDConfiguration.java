/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package io.github.yangziwen.pmd;

import io.github.yangziwen.pmd.renderers.RendererFactory;
import lombok.Getter;
import lombok.Setter;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.IOUtil;

public class PMDConfiguration extends net.sourceforge.pmd.PMDConfiguration {

    @Getter
    @Setter
    private String gitDir;

    @Getter
    @Setter
    private String baseRev;

    @Getter
    @Setter
    private boolean includeStagedCodes;

    @Getter
    @Setter
    private String excludeRegexp;

    @Override
    public Renderer createRenderer(boolean withReportWriter) {
        Renderer renderer = RendererFactory.createRenderer(getReportFormat(), getReportProperties());
        renderer.setShowSuppressedViolations(isShowSuppressedViolations());
        if (withReportWriter) {
            renderer.setWriter(IOUtil.createWriter(getReportFile()));
        }
        return renderer;
    }

}
