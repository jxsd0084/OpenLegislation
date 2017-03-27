package gov.nysenate.openleg.processor.bill.billstat;

import com.google.common.collect.Sets;
import gov.nysenate.openleg.model.base.PublishStatus;
import gov.nysenate.openleg.model.base.SessionYear;
import gov.nysenate.openleg.model.base.Version;
import gov.nysenate.openleg.model.bill.*;
import gov.nysenate.openleg.model.entity.Chamber;
import gov.nysenate.openleg.model.process.DataProcessUnit;
import gov.nysenate.openleg.model.sobi.SobiFragment;
import gov.nysenate.openleg.model.sobi.SobiFragmentType;
import gov.nysenate.openleg.processor.base.AbstractDataProcessor;
import gov.nysenate.openleg.processor.base.ParseError;
import gov.nysenate.openleg.processor.bill.BillActionAnalyzer;
import gov.nysenate.openleg.processor.bill.BillActionParser;
import gov.nysenate.openleg.processor.sobi.SobiProcessor;
import gov.nysenate.openleg.util.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by Robert Bebber on 3/20/17.
 */
@Service
public class BillStatProcessor extends AbstractDataProcessor implements SobiProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BillStatProcessor.class);
    @Autowired
    private XmlHelper xmlHelper;

    public BillStatProcessor() {
    }

    @Override
    public void init() {
        initBase();
    }

    @Override
    public SobiFragmentType getSupportedType() {
        return SobiFragmentType.BILLSTAT;
    }

    @Override
    public void process(SobiFragment sobiFragment) {
        logger.info("Processing BillStat...");
        LocalDateTime date = sobiFragment.getPublishedDateTime();
        logger.info("Processing " + sobiFragment.getFragmentId() + " (xml file).");
        DataProcessUnit unit = createProcessUnit(sobiFragment);
        try {
            final Document doc = xmlHelper.parse(sobiFragment.getText());
            final Node billTextNode = xmlHelper.getNode("billstatus", doc);
            final Integer sessyr = xmlHelper.getInteger("@sessyr", billTextNode);
            final String billhse = xmlHelper.getString("@billhse", billTextNode).trim();
            final Integer billno = xmlHelper.getInteger("@billno", billTextNode);
            final String action = xmlHelper.getString("@action", billTextNode).trim();
            final String sponsor = xmlHelper.getString("sponsor", billTextNode).trim();
            final String version = xmlHelper.getString("currentamd", billTextNode).trim();
            String lawSec = xmlHelper.getString("law", billTextNode).trim();
            String title = xmlHelper.getString("title", billTextNode).trim();
            String billactions = xmlHelper.getString("billactions", billTextNode).trim();
            Bill baseBill = getOrCreateBaseBill(sobiFragment.getPublishedDateTime(), new BillId(billhse +
                    billno, sessyr), sobiFragment);
            BillAmendment billAmendment = baseBill.getActiveAmendment();
            BillSponsor billSponsor = baseBill.getSponsor();
            Chamber chamber = baseBill.getBillType().getChamber();

            if (version == null || version.equals("")) {
                billAmendment = baseBill.getAmendment(Version.DEFAULT);
            } else {
                billAmendment = baseBill.getAmendment(Version.of(version));
            }
            if (action.equals("remove")) {
                removeCase(baseBill, billAmendment);
                return;
            }
            if (billSponsor == null) {
                billSponsor = new BillSponsor();
            }
            billSponsor.setMember(getMemberFromShortName(sponsor, new SessionYear(sessyr), chamber));
            baseBill.setSponsor(billSponsor);
            billAmendment.setLawSection(lawSec);
            baseBill.setTitle(title);

            List<BillAction> billActions = BillActionParser.parseActionsList(billAmendment.getBillId(), billactions);
            baseBill.setActions(billActions);
            // Use the BillActionAnalyzer to derive other data from the actions list.
            Optional<PublishStatus> defaultPubStatus = baseBill.getPublishStatus(Version.DEFAULT);
            BillActionAnalyzer analyzer = new BillActionAnalyzer(billAmendment.getBillId(), billActions, defaultPubStatus);
            analyzer.analyze();

            // Apply the results to the bill
            baseBill.setSubstitutedBy(analyzer.getSubstitutedBy().orElse(null));
            baseBill.setActiveVersion(analyzer.getActiveVersion());
            baseBill.setStatus(analyzer.getBillStatus());
            baseBill.setMilestones(analyzer.getMilestones());
            baseBill.setPastCommittees(analyzer.getPastCommittees());
            baseBill.setPublishStatuses(analyzer.getPublishStatusMap());
            analyzer.getSameAsMap().forEach((k, v) -> {
                if (baseBill.hasAmendment(k)) {
                    baseBill.getAmendment(k).setSameAs(Sets.newHashSet(v));
                }
            });
            billAmendment.setStricken(analyzer.isStricken());
            billIngestCache.set(baseBill.getBaseBillId(), baseBill, sobiFragment);
        } catch (IOException | SAXException | XPathExpressionException e) {
            throw new ParseError("Error While Parsing BillStatProcessorXML", e);
        }
    }

    /**
     * This method is responsible for removing key data fields impacted by the BillStat
     * processor.
     *
     * @param baseBill The Bill in alteration
     * @param billAmendment The Bill Amendment of said bill
     */
    private void removeCase(Bill baseBill, BillAmendment billAmendment) {
        billAmendment.setLawSection(null);
        baseBill.setSponsor(null);
        baseBill.setTitle(null);
    }

    @Override
    public void postProcess() {
        flushBillUpdates();
    }

}
