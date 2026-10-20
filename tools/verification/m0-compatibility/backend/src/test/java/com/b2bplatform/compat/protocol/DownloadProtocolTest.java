package com.b2bplatform.compat.protocol;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;

class DownloadProtocolTest extends ProtocolFixture {
    static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    Signer signer = new Signer();
    List<Grant> exposed = new ArrayList<>();
    @BeforeEach void readyDownload() throws Exception { publishedReference(); }
    long count(String table) throws Exception { return value("SELECT COUNT(*) FROM " + table + " WHERE site_id=?", SITE); }

    @Test void replayReturnsOriginalGrantWithoutResigningEvenAfterExpiryOrGateRevocation() throws Exception {
        Grant original = Forms.submit(801, "same-key", "ACCEPT", false, NOW, Failure.NONE, signer, exposed);
        assertThat(exposed).containsExactly(original);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
        committed(c -> Media.gate(c, null));
        Grant replay = Forms.submit(802, "same-key", "ACCEPT", false, NOW.plusSeconds(86400), Failure.NONE, signer, exposed);
        assertThat(replay).isEqualTo(original);
        assertThat(replay.expiresAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(signer.calls.get()).isEqualTo(1);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
    }
    @ParameterizedTest @EnumSource(value=Failure.class, names={"SIGN","TEMPLATE","ENQUEUE","BEFORE_COMMIT"})
    void everyPreCommitFailureRollsBackSubmissionGrantAndOutboxAndCanRetry(Failure fault) throws Exception {
        Throwable failure = catchThrowable(() -> Forms.submit(803, "failure-key", "ACCEPT", false, NOW, fault, signer, exposed));
        switch (fault) {
            case SIGN -> assertThat(failure).isInstanceOf(Rejected.class).hasMessage("INJECTED_SIGN_FAILURE");
            case TEMPLATE -> assertThat(failure).isInstanceOf(Rejected.class).hasMessage("NOTIFICATION_TEMPLATE_NOT_FOUND");
            case ENQUEUE -> { assertThat(failure).isInstanceOf(SQLException.class); assertThat(((SQLException) failure).getErrorCode()).isEqualTo(3819); }
            case BEFORE_COMMIT -> assertThat(failure).isInstanceOf(Rejected.class).hasMessage("INJECTED_COMMIT_BOUNDARY_FAILURE");
            default -> throw new AssertionError(fault);
        }
        assertThat(count("frm_submission")).isZero(); assertThat(count("ntf_message")).isZero(); assertThat(exposed).isEmpty();
        Grant result = Forms.submit(803, "failure-key", "ACCEPT", false, NOW, Failure.NONE, signer, exposed);
        assertThat(result).isNotNull(); assertThat(exposed).containsExactly(result);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
    }
    @ParameterizedTest @CsvSource({"QUARANTINE,false", "REJECT,false", "ACCEPT,true"})
    void quarantinedRejectedAndTestRequestsProduceNoGrantOrCustomerMail(String verdict, boolean test) throws Exception {
        assertThat(Forms.submit(804, "not-downloadable", verdict, test, NOW, Failure.NONE, signer, exposed)).isNull();
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isZero();
        assertThat(value("SELECT COUNT(*) FROM frm_submission WHERE download_grant IS NOT NULL AND site_id=?", SITE)).isZero();
        assertThat(signer.calls.get()).isZero(); assertThat(exposed).isEmpty();
    }
    @Test void missingActualTemplateRowRollsBackInsteadOfDeferringFailureToSmtp() throws Exception {
        committed(c -> update(c, "DELETE FROM ntf_template WHERE site_id=? AND locale='en'", SITE));
        assertThatThrownBy(() -> Forms.submit(805, "missing-template", "ACCEPT", false, NOW, Failure.NONE, signer, exposed)).isInstanceOf(Rejected.class).hasMessage("NOTIFICATION_TEMPLATE_NOT_FOUND");
        assertThat(count("frm_submission")).isZero(); assertThat(count("ntf_message")).isZero(); assertThat(exposed).isEmpty();
    }
    @Test void uncommittedSubmissionAndOutboxAreInvisibleToOtherConnections() throws Exception {
        try (var writing = connect()) {
            Grant grant = Forms.write(writing, 806, "uncommitted", "ACCEPT", false, NOW, Failure.NONE, signer);
            assertThat(grant).isNotNull();
            assertThat(count("frm_submission")).isZero(); assertThat(count("ntf_message")).isZero(); assertThat(exposed).isEmpty();
            writing.rollback();
        }
        assertThat(count("frm_submission")).isZero(); assertThat(count("ntf_message")).isZero();
    }
    @Test void lostHttpResponseAfterCommitReplaysWithoutSecondSubmissionOrSignature() throws Exception {
        Grant committedGrant;
        try (var c = connect()) {
            committedGrant = Forms.write(c, 813, "lost-response", "ACCEPT", false, NOW, Failure.NONE, signer);
            c.commit(); // simulate process/HTTP response loss after a successful database commit
        }
        assertThat(exposed).isEmpty();
        Grant retry = Forms.submit(814, "lost-response", "ACCEPT", false, NOW.plusSeconds(60), Failure.NONE, signer, exposed);
        assertThat(retry).isEqualTo(committedGrant);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
        assertThat(signer.calls.get()).isEqualTo(1); assertThat(exposed).containsExactly(committedGrant);
    }
    @Test void simultaneousSameKeyWaitsForWinnerAndReplaysInFreshTransaction() throws Exception {
        try (var winner = connect()) {
            Grant grant = Forms.write(winner, 807, "racing-key", "ACCEPT", false, NOW, Failure.NONE, signer);
            try (var loser = new Contender(c -> {
                assertThat(scalar(c, "SELECT COUNT(*) FROM frm_submission WHERE site_id=? AND idempotency_key='racing-key'", SITE)).isZero();
                try {
                    Forms.write(c, 808, "racing-key", "ACCEPT", false, NOW.plusSeconds(30), Failure.NONE, signer);
                    throw new AssertionError("Unique key must prevent a second submission");
                } catch (SQLIntegrityConstraintViolationException conflict) {
                    assertThat(conflict.getErrorCode()).isEqualTo(1062);
                    // The losing transaction still has its original RR snapshot.
                    assertThat(scalar(c, "SELECT COUNT(*) FROM frm_submission WHERE site_id=? AND idempotency_key='racing-key'", SITE)).isZero();
                    c.rollback();
                    try (var fresh = connect()) { assertThat(Forms.replay(fresh, "racing-key")).isEqualTo(grant); }
                }
            })) {
                loser.awaitDatabaseLockWait(); winner.commit(); loser.succeeds();
            }
        }
        Grant replay = Forms.submit(809, "racing-key", "ACCEPT", false, NOW.plusSeconds(60), Failure.NONE, signer, exposed);
        assertThat(exposed).containsExactly(replay);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
        assertThat(signer.calls.get()).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"gate", "reference", "scan"})
    void currentGateReferenceAndScanAreRequiredBeforeGrant(String invalid) throws Exception {
        committed(c -> {
            if (invalid.equals("gate")) Media.gate(c, null);
            if (invalid.equals("reference")) update(c, "DELETE FROM mda_usage WHERE site_id=? AND media_id=?", SITE, MEDIA);
            if (invalid.equals("scan")) update(c, "UPDATE mda_media SET status='PROCESSING' WHERE id=?", MEDIA);
        });
        assertThatThrownBy(() -> Forms.submit(810, "invalid-gate", "ACCEPT", false, NOW, Failure.NONE, signer, exposed)).isInstanceOf(Rejected.class);
        assertThat(count("frm_submission")).isZero(); assertThat(count("ntf_message")).isZero();
        assertThat(signer.calls.get()).isZero(); assertThat(exposed).isEmpty();
    }
    @Test void smtpFailureAfterCommitRetainsOriginalGrantAndQueuedObligation() throws Exception {
        Grant grant = Forms.submit(811, "smtp-failure", "ACCEPT", false, NOW, Failure.NONE, signer, exposed);
        committed(c -> update(c, "UPDATE ntf_message SET status='FAILED',attempts=6,error='Injected SMTP failure' WHERE site_id=?", SITE));
        assertThat(Forms.submit(812, "smtp-failure", "ACCEPT", false, NOW.plusSeconds(30), Failure.NONE, signer, exposed)).isEqualTo(grant);
        assertThat(count("frm_submission")).isEqualTo(1); assertThat(count("ntf_message")).isEqualTo(1);
        assertThat(field("SELECT status FROM ntf_message WHERE site_id=?", SITE)).isEqualTo("FAILED");
        assertThat(signer.calls.get()).isEqualTo(1);
    }
}
