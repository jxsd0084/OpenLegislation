package gov.nysenate.openleg.dao.spotcheck;

import com.google.common.collect.Sets;
import gov.nysenate.openleg.dao.base.*;
import gov.nysenate.openleg.model.base.SessionYear;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.service.spotcheck.base.MismatchStatusService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static gov.nysenate.openleg.dao.spotcheck.SqlSpotCheckReportQuery.*;
import static gov.nysenate.openleg.util.DateUtils.toDate;

/**
 * The AbstractSpotCheckReportDao implements all the functionality required by SpotCheckReportDao
 * regardless of the content key specified. This class must be subclasses with a concrete type for
 * the ContentKey. The subclass will need to handle just the conversions for the ContentKey class.
 *
 * @param <ContentKey>
 */
public abstract class AbstractSpotCheckReportDao<ContentKey> extends SqlBaseDao
        implements SpotCheckReportDao<ContentKey> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSpotCheckReportDao.class);

    /** --- Abstract Methods --- */

    /**
     * Subclasses should implement this conversion from a Map containing certain key/val pairs to
     * an instance of ContentKey. This is needed since the keys are stored as an hstore in the
     * database.
     *
     * @param keyMap Map<String, String>
     * @return ContentKey
     */
    public abstract ContentKey getKeyFromMap(Map<String, String> keyMap);

    /**
     * Subclasses should implement a conversion from an instance of ContentKey to a Map of
     * key/val pairs that fully represent that ContentKey.
     *
     * @param key ContentKey
     * @return Map<String, String>
     */
    public abstract Map<String, String> getMapFromKey(ContentKey key);

    /** --- Implemented Methods --- */

    /**
     * {@inheritDoc}
     */
    @Override
    public SpotCheckReport<ContentKey> getReport(SpotCheckReportId id) throws DataAccessException {
        return null; // TODO: WIP
//        ImmutableParams reportIdParams = ImmutableParams.from(new MapSqlParameterSource()
//                .addValue("referenceType", id.getReferenceType().name())
//                .addValue("reportDateTime", toDate(id.getReportDateTime())));
//        // Check for the report record or throw a DataAccessException if not present
//        SpotCheckReport<ContentKey> report =
//                jdbcNamed.queryForObject(SELECT_REPORT.getSql(schema()), reportIdParams, (rs, row) ->
//                        new SpotCheckReport<>(
//                                new SpotCheckReportId(SpotCheckRefType.valueOf(rs.getString("reference_type")),
//                                        getLocalDateTimeFromRs(rs, "reference_date_time"),
//                                        getLocalDateTimeFromRs(rs, "report_date_time")),
//                                rs.getString("notes")
//                        )
//                );
//        // Obtain all the current and prior observations/mismatches
//        ReportObservationsHandler handler = new ReportObservationsHandler();
//        jdbcNamed.query(SELECT_OBS_MISMATCHES_BY_REPORT.getSql(schema()), reportIdParams, handler);
//        report.setObservations(handler.getObsMap());
//        return report;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SpotCheckReportSummary> getReportSummaries(SpotCheckRefType refType, LocalDateTime start, LocalDateTime end,
                                                           SortOrder dateOrder) {
        return null; // TODO: WIP
//        ImmutableParams params = ImmutableParams.from(new MapSqlParameterSource()
//                .addValue("startDateTime", toDate(start))
//                .addValue("endDateTime", toDate(end))
//                .addValue("getAllRefTypes", refType == null)
//                .addValue("referenceType", refType != null ? refType.toString() : ""));
//        ReportSummaryHandler handler = new ReportSummaryHandler(dateOrder);
//        jdbcNamed.query(SELECT_REPORT_SUMMARIES.getSql(schema()), params, handler);
//        return handler.getSummaries();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaginatedList<DeNormSpotCheckMismatch> getMismatches(MismatchQuery query, LimitOffset limitOffset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("datasource", query.getDataSource().name())
                .addValue("contentTypes", query.getContentTypes().stream().map(Enum::name).collect(Collectors.toSet()))
                .addValue("statuses", query.getMismatchStatuses().stream().map(Enum::name).collect(Collectors.toSet()))
                .addValue("ignoreStatuses", query.getIgnoredStatuses().stream().map(Enum::name).collect(Collectors.toSet()))
                .addValue("toDate", query.getToDate())
                .addValue("fromDate", query.getFromDate());
        String sql = SqlSpotCheckReportQuery.GET_MISMATCHES.getSql(schema(), limitOffset);
        PaginatedRowHandler<DeNormSpotCheckMismatch> handler = new PaginatedRowHandler<>(limitOffset, "total_rows", new OpenMismatchMapper());
        jdbcNamed.query(sql, params, handler);
        return handler.getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MismatchSummary getMismatchSummary(SpotCheckDataSource datasource, LocalDateTime summaryDateTime) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("datasource", datasource.name())
                .addValue("fromDate", SessionYear.of(summaryDateTime.getYear()).asDateTimeRange().lowerEndpoint())
                .addValue("toDate", summaryDateTime)
                .addValue("startOfToDate", summaryDateTime.truncatedTo(ChronoUnit.DAYS));
        String sql = SqlSpotCheckReportQuery.MISMATCH_SUMMARY.getSql(schema());
        MismatchSummaryHandler summaryHandler = new MismatchSummaryHandler();
        jdbcNamed.query(sql, params, summaryHandler);
        return summaryHandler.getSummary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveReport(SpotCheckReport<ContentKey> report) {
        int reportId = insertReport(report);
        // Return early if the observations have not been set
        if (report.getObservations() == null) {
            logger.warn("The observations have not been set on this report.");
            return;
        }
        // Get the Keys and MismatchTypes checked in this report. (Used in calculating resolved mismatches)
        Set<Object> checkedKeys = report.getObservations().values().stream().map(SpotCheckObservation::getKey).collect(Collectors.toSet());
        Set<SpotCheckMismatchType> checkedTypes = report.getReferenceType().checkedMismatchTypes();

        List<DeNormSpotCheckMismatch> reportMismatches = reportToDeNormMismatches(report, reportId);
        MismatchQuery query = new MismatchQuery(report.getReferenceType().getDataSource(), Sets.newHashSet(report.getReferenceType().getContentType()));
        query.withToDate(report.getReferenceDateTime());
        List<DeNormSpotCheckMismatch> currentMismatches = getMismatches(query, LimitOffset.ALL).getResults();

        List<DeNormSpotCheckMismatch> updatedMismatches = MismatchStatusService.deriveStatuses(reportMismatches, currentMismatches);
        updatedMismatches.addAll(MismatchStatusService.deriveResolved(reportMismatches, currentMismatches, checkedKeys, checkedTypes));

        insertMismatches(updatedMismatches);
    }

    private int insertReport(SpotCheckReport<ContentKey> report) {
        ImmutableParams reportParams = ImmutableParams.from(getReportIdParams(report));
        KeyHolder reportIdHolder = new GeneratedKeyHolder();
        jdbcNamed.update(INSERT_REPORT.getSql(schema()), reportParams, reportIdHolder, new String[]{"id"});
        return reportIdHolder.getKey().intValue();
    }

    private void insertMismatches(List<DeNormSpotCheckMismatch> mismatches) {
        List<MapSqlParameterSource> params = mismatches.stream()
                .map(this::mismatchParams)
                .collect(Collectors.toList());
        String sql = INSERT_MISMATCH.getSql(schema());
        jdbcNamed.batchUpdate(sql, params.stream().toArray(MapSqlParameterSource[]::new));
    }

    private MapSqlParameterSource mismatchParams(DeNormSpotCheckMismatch mismatch) {
        return new MapSqlParameterSource()
                .addValue("key", toHstoreString(getMapFromKey((ContentKey) mismatch.getKey())))
                .addValue("mismatchType", mismatch.getMismatchType().name())
                .addValue("reportId", mismatch.getReportId())
                .addValue("datasource", mismatch.getDataSource().name())
                .addValue("contentType", mismatch.getContentType().name())
                .addValue("referenceType", mismatch.getReferenceId().getReferenceType().name())
                .addValue("mismatchStatus", mismatch.getStatus().name())
                .addValue("referenceData", mismatch.getReferenceData())
                .addValue("observedData", mismatch.getObservedData())
                .addValue("notes", mismatch.getNotes())
                .addValue("issueIds", "{" + StringUtils.join(mismatch.getIssueIds(), ',') + "}")
                .addValue("ignoreLevel", mismatch.getIgnoreStatus().name())
                .addValue("reportDateTime", mismatch.getReportDateTime())
                .addValue("observedDateTime", mismatch.getObservedDateTime())
                .addValue("referenceActiveDateTime", mismatch.getReferenceId().getRefActiveDateTime());
    }

    private List<DeNormSpotCheckMismatch> reportToDeNormMismatches(SpotCheckReport<ContentKey> report, int reportId) {
        List<DeNormSpotCheckMismatch> mismatches = new ArrayList<>();
        for (SpotCheckObservation<ContentKey> ob : report.getObservations().values()) {
            // Skip if no mismatches in the observation
            if (ob.getMismatches().size() == 0) {
                continue;
            }
            for (SpotCheckMismatch m : ob.getMismatches().values()) {
                DeNormSpotCheckMismatch mismatch = new DeNormSpotCheckMismatch<>(ob.getKey(), m.getMismatchType(),
                        report.getReferenceType().getDataSource());
                mismatch.setReportId(reportId);
                mismatch.setContentType(report.getReferenceType().getContentType());
                mismatch.setReferenceId(ob.getReferenceId());
                mismatch.setReferenceData(m.getReferenceData());
                mismatch.setObservedData(m.getObservedData());
                mismatch.setNotes(m.getNotes());
                mismatch.setObservedDateTime(ob.getObservedDateTime());
                mismatch.setReportDateTime(report.getReportDateTime());
                mismatch.setIgnoreStatus(m.getIgnoreStatus());
                mismatch.setIssueIds(new HashSet<>(m.getIssueIds()));
                mismatches.add(mismatch);
            }
        }
        return mismatches;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteReport(SpotCheckReportId reportId) {
        // TODO WIP
//        ImmutableParams reportIdParams = ImmutableParams.from(new MapSqlParameterSource()
//                .addValue("referenceType", reportId.getReferenceType().name())
//                .addValue("reportDateTime", toDate(reportId.getReportDateTime())));
//        jdbcNamed.update(DELETE_REPORT.getSql(schema()), reportIdParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMismatchIgnoreStatus(int mismatchId, SpotCheckMismatchIgnore ignoreStatus) {
        // TODO: WIP
//        MapSqlParameterSource params = new MapSqlParameterSource()
//                .addValue("mismatchId", mismatchId)
//                .addValue("ignoreLevel", Optional.ofNullable(ignoreStatus).map(SpotCheckMismatchIgnore::getCode).orElse(null));
//        if (ignoreStatus == null || ignoreStatus == SpotCheckMismatchIgnore.NOT_IGNORED) {
//            jdbcNamed.update(DELETE_MISMATCH_IGNORE.getSql(schema()), params);
//        } else {
//            if (jdbcNamed.update(UPDATE_MISMATCH_IGNORE.getSql(schema()), params) == 0) {
//                jdbcNamed.update(INSERT_MISMATCH_IGNORE.getSql(schema()), params);
//            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIssueId(int mismatchId, String issueId) {
        // TODO WIP
//        SqlParameterSource params = getIssueIdParams(mismatchId, issueId);
//        jdbcNamed.update(ADD_ISSUE_ID.getSql(schema()), params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIssueId(int mismatchId, String issueId) {
        // TODO WIP
//        SqlParameterSource params = getIssueIdParams(mismatchId, issueId);
//        jdbcNamed.update(DELETE_ISSUE_ID.getSql(schema()), params);
    }

    /**
     * --- Helper Classes ---
     */

    private class OpenMismatchMapper implements RowMapper<DeNormSpotCheckMismatch> {

        @Override
        public DeNormSpotCheckMismatch<ContentKey> mapRow(ResultSet rs, int rowNum) throws SQLException {
            ContentKey key = getKeyFromMap(getHstoreMap(rs, "key"));
            SpotCheckMismatchType type = SpotCheckMismatchType.valueOf(rs.getString("mismatch_type"));
            SpotCheckDataSource dataSource = SpotCheckDataSource.valueOf(rs.getString("datasource"));
            DeNormSpotCheckMismatch mismatch = new DeNormSpotCheckMismatch<>(key, type, dataSource);
            mismatch.setMismatchId(rs.getInt("mismatch_id"));
            mismatch.setReportId(rs.getInt("report_id"));
            mismatch.setStatus(SpotCheckMismatchStatus.valueOf(rs.getString("mismatch_status")));
            mismatch.setContentType(SpotCheckContentType.valueOf(rs.getString("content_type")));
            mismatch.setReferenceData(rs.getString("reference_data"));
            mismatch.setObservedData(rs.getString("observed_data"));
            mismatch.setReportDateTime(getLocalDateTimeFromRs(rs, "report_date_time"));
            mismatch.setObservedDateTime(getLocalDateTimeFromRs(rs, "observed_date_time"));
            mismatch.setNotes(rs.getString("notes"));
            mismatch.setIgnoreStatus(SpotCheckMismatchIgnore.valueOf(rs.getString("ignore_level")));
            mismatch.setIssueIds(Sets.newHashSet(rs.getArray("issue_ids").getArray()));

            SpotCheckRefType refType = SpotCheckRefType.valueOf(rs.getString("reference_type"));
            LocalDateTime refActiveDateTime = getLocalDateTimeFromRs(rs, "reference_active_date_time");
            mismatch.setReferenceId(new SpotCheckReferenceId(refType, refActiveDateTime));
            return mismatch;
        }
    }

    private class MismatchSummaryHandler implements RowCallbackHandler {

        private MismatchSummary summary = new MismatchSummary();

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            SpotCheckContentType contentType = SpotCheckContentType.valueOf(rs.getString("content_type"));
            SpotCheckMismatchStatus status = SpotCheckMismatchStatus.valueOf(rs.getString("mismatch_status"));
            int count = rs.getInt("count");

            SpotCheckMismatchStatusSummary statusSummary = new SpotCheckMismatchStatusSummary(status, contentType, count);
            summary.addStatusSummary(statusSummary);
        }

        protected MismatchSummary getSummary() {
            return summary;
        }
    }

    protected static final RowMapper<SpotCheckReportId> reportIdRowMapper = (rs, row) ->
            new SpotCheckReportId(SpotCheckRefType.valueOf(rs.getString("reference_type")),
                    getLocalDateTimeFromRs(rs, "reference_date_time"),
                    getLocalDateTimeFromRs(rs, "report_date_time"));

//    protected class ReportSummaryHandler extends SummaryHandler {
//        private Map<SpotCheckReportId, SpotCheckReportSummary> summaryMap;
//
//        public ReportSummaryHandler(SortOrder order) {
//            summaryMap = new TreeMap<>((a, b) -> a.compareTo(b) * (SortOrder.ASC.equals(order) ? 1 : -1));
//        }
//
//        @Override
//        protected SpotCheckSummary getRelevantSummary(ResultSet rs) throws SQLException {
//            SpotCheckReportId id = reportIdRowMapper.mapRow(rs, rs.getRow());
//            if (!summaryMap.containsKey(id)) {
//                summaryMap.put(id, new SpotCheckReportSummary(id, rs.getString("notes")));
//            }
//            return summaryMap.get(id);
//        }
//
//        public List<SpotCheckReportSummary> getSummaries() {
//            return new ArrayList<>(summaryMap.values());
//        }
//    }

    /**
     * --- Param Source Methods ---
     */

    private MapSqlParameterSource getReportIdParams(SpotCheckReport<ContentKey> report) {
        return new MapSqlParameterSource()
                .addValue("referenceType", report.getReferenceType().name())
                .addValue("reportDateTime", toDate(report.getReportDateTime()))
                .addValue("referenceDateTime", toDate(report.getReferenceDateTime()))
                .addValue("notes", report.getNotes());
    }

    private MapSqlParameterSource getIssueIdParams(int mismatchId, String issueId) {
        return new MapSqlParameterSource()
                .addValue("mismatchId", mismatchId)
                .addValue("issueId", issueId);
    }
}