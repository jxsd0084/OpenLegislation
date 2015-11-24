package gov.nysenate.openleg.client.view.spotcheck;

import gov.nysenate.openleg.client.view.base.ViewObject;
import gov.nysenate.openleg.model.spotcheck.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class ReportInfoView extends SpotCheckSummaryView implements ViewObject
{

    protected String referenceType;
    protected LocalDateTime referenceDateTime;
    protected LocalDateTime reportDateTime;
    protected String notes;
    protected int observedCount;

    public ReportInfoView(SpotCheckReportSummary summary) {
        this(summary,
                summary != null ? summary.getNotes() : null,
                summary != null ? summary.getObservedCount() : 0,
                summary != null ? summary.getReportId() : null);
    }

    public ReportInfoView(SpotCheckReport<?> report) {
        this(Optional.ofNullable(report));
    }

    private ReportInfoView(Optional<SpotCheckReport<?>> reportOpt) {
        this(reportOpt.map(SpotCheckReport::getSummary).orElse(null),
                reportOpt.map(SpotCheckReport::getNotes).orElse(null),
                reportOpt.map(SpotCheckReport::getObservedCount).orElse(0),
                reportOpt.map(SpotCheckReport::getReportId).orElse(null));
    }

    protected ReportInfoView(SpotCheckSummary summary, String notes, int observedCount, SpotCheckReportId reportId) {
        super(summary);
        this.notes = notes;
        this.observedCount = observedCount;

        Optional<SpotCheckReportId> reportIdOpt = Optional.ofNullable(reportId);
        this.referenceType = reportIdOpt.map(SpotCheckReportId::getReferenceType)
                .map(Enum::name).orElse(null);
        this.referenceDateTime = reportIdOpt.map(SpotCheckReportId::getReferenceDateTime).orElse(null);
        this.reportDateTime = reportIdOpt.map(SpotCheckReportId::getReportDateTime).orElse(null);
    }

    /** --- Getters --- */

    public String getNotes() {
        return notes;
    }

    public int getObservedCount() {
        return observedCount;
    }

    @Override
    public String getViewType() {
        return referenceType + " report-info";
    }

    public String getReferenceType() {
        return referenceType;
    }

    public LocalDateTime getReferenceDateTime() {
        return referenceDateTime;
    }

    public LocalDateTime getReportDateTime() {
        return reportDateTime;
    }
}