package com.b2bplatform.compat.protocol;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;

class MediaProtocolTest extends ProtocolFixture {
    @TempDir Path store;
    Path blob() throws Exception { return Files.writeString(store.resolve("probe-object"), "blob"); }

    @Test void saveFirstBlocksDeleteThenRejectsIt() throws Exception {
        try (var saving = connect()) {
            Media.reference(saving, SITE, false);
            try (var deleting = new Contender(Media::softDelete)) {
                deleting.awaitDatabaseLockWait(); saving.commit(); deleting.rejected("MEDIA_IN_USE");
            }
        }
        assertThat(value("SELECT COUNT(*) FROM mda_usage WHERE media_id=?", MEDIA)).isEqualTo(1);
        assertThat(value("SELECT COUNT(*) FROM mda_media WHERE id=? AND deleted_at IS NULL", MEDIA)).isEqualTo(1);
    }
    @Test void deleteFirstBlocksSaveThenRejectsItAndRetainsObject() throws Exception {
        Path blob = blob();
        try (var deleting = connect()) {
            Media.softDelete(deleting);
            try (var saving = new Contender(c -> Media.reference(c, SITE, false))) {
                saving.awaitDatabaseLockWait(); deleting.commit(); saving.rejected("MEDIA_DELETED");
            }
        }
        assertThat(Files.exists(blob)).isTrue();
        assertThat(value("SELECT COUNT(*) FROM mda_usage WHERE media_id=?", MEDIA)).isZero();
        assertThat(value("SELECT download_gate_version FROM mda_media WHERE id=?", MEDIA)).isEqualTo(1);
    }
    @Test void repeatableReadSnapshotMissesNewReferenceButCurrentReadProtectsDelete() throws Exception {
        try (var deleting = connect()) {
            assertThat(scalar(deleting, "SELECT COUNT(*) FROM mda_usage WHERE media_id=?", MEDIA)).isZero();
            committed(c -> Media.reference(c, SITE, false));
            // Negative control: an ordinary RR read still says zero after save committed.
            assertThat(scalar(deleting, "SELECT COUNT(*) FROM mda_usage WHERE media_id=?", MEDIA)).isZero();
            assertThatThrownBy(() -> Media.softDelete(deleting)).isInstanceOf(Rejected.class).hasMessage("MEDIA_IN_USE");
            deleting.rollback();
        }
        assertThat(value("SELECT COUNT(*) FROM mda_media WHERE deleted_at IS NULL AND id=?", MEDIA)).isEqualTo(1);
    }
    @Test void restoreFirstMakesWaitingPurgeIneligible() throws Exception {
        Path blob = blob(); olderTrash();
        try (var restoring = connect()) {
            Media.restore(restoring, blob);
            try (var purging = new Contender(Media::registerPurge)) {
                purging.awaitDatabaseLockWait(); restoring.commit(); purging.rejected("MEDIA_RETENTION");
            }
        }
        assertThat(value("SELECT COUNT(*) FROM sys_job WHERE id=?", PURGE)).isZero();
        assertThat(Files.exists(blob)).isTrue();
    }
    @Test void purgeFirstBlocksRestoreAndSurvivesDeleteFailureAndLostAcknowledgment() throws Exception {
        Path blob = blob(); olderTrash();
        try (var purging = connect()) {
            Media.registerPurge(purging);
            try (var restoring = new Contender(c -> Media.restore(c, blob))) {
                restoring.awaitDatabaseLockWait(); purging.commit(); restoring.rejected("MEDIA_PURGE_STARTED");
            }
        }
        assertThatThrownBy(() -> Media.purgeObject(blob, true, false)).isInstanceOf(java.io.IOException.class);
        assertThat(Files.exists(blob)).isTrue();
        assertThat(value("SELECT purge_job_id FROM mda_media WHERE id=?", MEDIA)).isEqualTo(PURGE);
        assertThat(field("SELECT status FROM sys_job WHERE id=?", PURGE)).isEqualTo("PENDING");
        assertThatThrownBy(() -> Media.purgeObject(blob, false, true)).isInstanceOf(java.io.IOException.class);
        assertThat(Files.exists(blob)).isFalse();
        assertThatThrownBy(() -> committed(c -> Media.restore(c, blob))).isInstanceOf(Rejected.class).hasMessage("MEDIA_PURGE_STARTED");
        Media.purgeObject(blob, false, false); // object already absent; acknowledgment may safely retry
        assertThat(value("SELECT COUNT(*) FROM mda_media WHERE id=?", MEDIA)).isZero();
        assertThat(field("SELECT status FROM sys_job WHERE id=?", PURGE)).isEqualTo("SUCCEEDED");
    }
    @Test void rolledBackPurgeLeavesNoTaskOrMarkerAndAllowsRestore() throws Exception {
        Path blob = blob(); olderTrash();
        try (var c = connect()) { Media.registerPurge(c); c.rollback(); }
        assertThat(value("SELECT COUNT(*) FROM sys_job WHERE id=?", PURGE)).isZero();
        assertThat(field("SELECT purge_job_id FROM mda_media WHERE id=?", MEDIA)).isNull();
        committed(c -> Media.restore(c, blob));
        assertThat(value("SELECT COUNT(*) FROM mda_media WHERE id=? AND deleted_at IS NULL", MEDIA)).isEqualTo(1);
        assertThat(Files.exists(blob)).isTrue();
    }
    @ParameterizedTest @CsvSource({"PROCESSING,false,true", "PROCESSING,true,false", "QUARANTINED,false,false", "READY,true,true"})
    void referenceChecksScanStateAndPublicationStage(String status, boolean published, boolean allowed) throws Exception {
        committed(c -> update(c, "UPDATE mda_media SET status=? WHERE id=?", status, MEDIA));
        if (allowed) committed(c -> Media.reference(c, SITE, published));
        else assertThatThrownBy(() -> committed(c -> Media.reference(c, SITE, published))).isInstanceOf(Rejected.class).hasMessage("MEDIA_NOT_READY");
        assertThat(value("SELECT COUNT(*) FROM mda_usage WHERE media_id=?", MEDIA)).isEqualTo(allowed ? 1 : 0);
    }
    @Test void crossSiteMediaCannotBeReferenced() {
        assertThatThrownBy(() -> committed(c -> Media.reference(c, SITE + 1, false))).isInstanceOf(Rejected.class).hasMessage("NOT_FOUND");
    }
}
