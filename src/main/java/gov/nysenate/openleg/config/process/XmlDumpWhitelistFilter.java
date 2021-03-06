package gov.nysenate.openleg.config.process;

import gov.nysenate.openleg.model.sourcefiles.SourceType;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiBlock;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiFragment;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiFragmentType;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiLineType;

import static gov.nysenate.openleg.model.sourcefiles.SourceType.*;
import static gov.nysenate.openleg.model.sourcefiles.sobi.SobiFragmentType.BILLTEXT;

/**
 * Filter for the time during an xml data dump.
 *
 * Because the nature of our data source's record keeping, data received as a
 * dump (as opposed to received gradually over time as updates), does not contain
 * as good of a historical record of changes.
 * But some of the enhancements of XML are still desirable and will still be used despite the dump drawbacks.
 */
public class XmlDumpWhitelistFilter implements SourceFilter {

    @Override
    public boolean acceptFragment(SobiFragment sobiFragment) {
        SourceType sourceType = sobiFragment.getParentSobiFile().getSourceType();
        SobiFragmentType fragmentType = sobiFragment.getType();
        // Accept all SOBI fragments and xml BILLTEXT fragments
        return sourceType == SOBI || sourceType == XML && fragmentType == BILLTEXT;
    }

    @Override
    public boolean acceptBlock(SobiBlock sobiBlock) {
        // accept all SOBI blocks that aren't text
        return sobiBlock.getType() != SobiLineType.TEXT;
    }
}
