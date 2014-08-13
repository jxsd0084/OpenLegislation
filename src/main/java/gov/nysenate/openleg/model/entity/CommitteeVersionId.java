package gov.nysenate.openleg.model.entity;

import com.google.common.collect.ComparisonChain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

public class CommitteeVersionId extends CommitteeId implements Serializable
{
    private static final long serialVersionUID = 2679527346305021089L;

    /** The session year this committee is referenced in. */
    private int session;

    /** Refers to the date this committee was referenced. */
    private LocalDate referenceDate;

    /** --- Constructors --- */

    public CommitteeVersionId(Chamber chamber, String name, int session, LocalDate referenceDate) {
        super(chamber, name);
        if (referenceDate == null) {
            throw new IllegalArgumentException("referenceDate cannot be null!");
        }
        this.session = session;
        this.referenceDate = referenceDate;
    }

    /** --- Overrides --- */

    @Override
    public String toString() {
        return super.toString() + '-' + session + '-' + referenceDate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommitteeVersionId)) return false;
        if (!super.equals(o)) return false;
        CommitteeVersionId that = (CommitteeVersionId) o;
        if (session != that.session) return false;
        if (!referenceDate.equals(that.referenceDate)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + session;
        result = 31 * result + referenceDate.hashCode();
        return result;
    }

    @Override
    public int compareTo(CommitteeId o) {
        CommitteeVersionId cvId = (CommitteeVersionId) o;
        return ComparisonChain.start()
           .compare(this.referenceDate, cvId.referenceDate)
           .compare(super.getName(), o.getName())
           .compare(super.getChamber(), o.getChamber())
           .result();
    }

    /** --- Basic Getters/Setters --- */

    public int getSession() {
        return session;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }
}