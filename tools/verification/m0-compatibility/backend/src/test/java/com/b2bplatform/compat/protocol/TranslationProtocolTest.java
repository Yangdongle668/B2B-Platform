package com.b2bplatform.compat.protocol;

import org.junit.jupiter.api.Test;
import static com.b2bplatform.compat.protocol.ProtocolModel.*;
import static org.assertj.core.api.Assertions.*;

class TranslationProtocolTest extends ProtocolFixture {
    void originalContributions() throws Exception {
        assertThat(field("SELECT title_override FROM seo_meta WHERE localization_id=?", TARGET)).isEqualTo("original");
        assertThat(field("SELECT JSON_UNQUOTE(JSON_EXTRACT(document,'$.body')) FROM pg_layout WHERE content_id=? AND locale='en'", CONTENT)).isEqualTo("original");
    }
    @Test void manualEditRejectsCapturedOldResultWithoutRefreshingVersion() throws Exception {
        capture();
        committed(c -> { Content.target(c, 0); update(c, "UPDATE cnt_localization SET draft_title='manual-edit',version=version+1 WHERE id=?", TARGET); });
        committed(c -> assertThat(Translation.apply(c, "worker-a", false)).isFalse());
        assertThat(value("SELECT expected_target_version FROM trn_job_item WHERE id=?", ITEM)).isZero();
        assertThat(field("SELECT status FROM trn_job_item WHERE id=?", ITEM)).isEqualTo("SUPERSEDED");
        assertThat(field("SELECT draft_title FROM cnt_localization WHERE id=?", TARGET)).isEqualTo("manual-edit");
        originalContributions();
    }
    @Test void contributorsCommitTogetherAndIncrementCommonVersionExactlyOnce() throws Exception {
        capture(); committed(c -> assertThat(Translation.apply(c, "worker-a", false)).isTrue());
        assertThat(value("SELECT version FROM cnt_localization WHERE id=?", TARGET)).isEqualTo(1);
        try (var c = connect()) {
            String body = (String) one(c, "SELECT JSON_UNQUOTE(JSON_EXTRACT(document,'$.body')) AS body FROM pg_layout WHERE content_id=? AND locale='en'", CONTENT).get("body");
            String seo = (String) one(c, "SELECT title_override FROM seo_meta WHERE localization_id=?", TARGET).get("title_override");
            assertThat(one(c, "SELECT draft_hash FROM cnt_localization WHERE id=?", TARGET).get("draft_hash")).isEqualTo(hash(body + "|" + seo));
            assertThat(Content.completionAllowed(c, 1, 1, false)).isTrue();
            assertThat(Content.completionAllowed(c, 1, 1, true)).isFalse();
        }
        assertThat(field("SELECT status FROM trn_job_item WHERE id=?", ITEM)).isEqualTo("SUCCEEDED");
    }
    @Test void providerFailureRollsBackAllContributionsAndTaskCompletion() throws Exception {
        capture();
        assertThatThrownBy(() -> committed(c -> Translation.apply(c, "worker-a", true))).isInstanceOf(Rejected.class).hasMessage("INJECTED_PROVIDER_FAILURE");
        originalContributions();
        assertThat(value("SELECT version FROM cnt_localization WHERE id=?", TARGET)).isZero();
        assertThat(field("SELECT status FROM trn_job_item WHERE id=?", ITEM)).isEqualTo("RUNNING");
    }
    @Test void policyFirstBlocksApplyThenInvalidatesAllLocalesAndPendingApproval() throws Exception {
        capture();
        committed(c -> {
            update(c, "UPDATE cnt_localization SET draft_state='IN_REVIEW' WHERE id=?", TARGET);
            update(c, "UPDATE cnt_localization SET draft_state='APPROVED' WHERE id=?", SOURCE);
            update(c, "INSERT INTO cnt_review(id,site_id,localization_id,draft_hash) VALUES(?,?,?,?)", 790, SITE, TARGET, hash("original"));
        });
        try (var changing = connect()) {
            Content.policy(changing, 0, true);
            try (var translating = new Contender(c -> assertThat(Translation.apply(c, "worker-a", false)).isFalse())) {
                translating.awaitDatabaseLockWait(); changing.commit(); translating.succeeds();
            }
        }
        assertThat(value("SELECT COUNT(*) FROM cnt_localization WHERE content_id=? AND version=1 AND draft_state='EDITING'", CONTENT)).isEqualTo(2);
        assertThat(field("SELECT status FROM cnt_review WHERE id=790")).isEqualTo("CANCELED");
        originalContributions();
    }
    @Test void applyFirstBlocksPolicyAndLateCompletionCannotAutoPublish() throws Exception {
        capture();
        try (var translating = connect()) {
            assertThat(Translation.apply(translating, "worker-a", false)).isTrue();
            try (var changing = new Contender(c -> Content.policy(c, 0, true))) {
                changing.awaitDatabaseLockWait(); translating.commit(); changing.succeeds();
            }
        }
        assertThat(value("SELECT version FROM cnt_localization WHERE id=?", TARGET)).isEqualTo(2);
        try (var c = connect()) { assertThat(Content.completionAllowed(c, 1, 1, false)).isFalse(); }
        assertThat(field("SELECT template_key FROM pg_page WHERE content_id=?", CONTENT)).isEqualTo("legal");
    }
    @Test void legalPolicyAndPolicyAbaBothRejectLateCompletion() throws Exception {
        committed(c -> Content.policy(c, 0, true));
        capture(); committed(c -> assertThat(Translation.apply(c, "worker-a", false)).isTrue());
        // Matching target/source versions are insufficient: current legal policy still denies.
        try (var c = connect()) { assertThat(Content.completionAllowed(c, 2, 1, false)).isFalse(); }
        committed(c -> Content.policy(c, 1, false));
        try (var c = connect()) { assertThat(Content.completionAllowed(c, 2, 1, false)).isFalse(); }
        assertThat(value("SELECT version FROM cnt_content WHERE id=?", CONTENT)).isEqualTo(2);
    }
    @Test void missingCapturedVersionCannotWriteAnyProvider() throws Exception {
        committed(c -> update(c, "UPDATE trn_job_item SET status='RUNNING',worker_id='worker-a' WHERE id=?", ITEM));
        assertThatThrownBy(() -> committed(c -> Translation.apply(c, "worker-a", false))).isInstanceOf(Rejected.class).hasMessage("CONTENT_VERSION_REQUIRED");
    }
    @Test void lostLeaseCannotWriteAnyProvider() throws Exception {
        capture();
        committed(c -> update(c, "UPDATE trn_job_item SET worker_id='worker-b' WHERE id=?", ITEM));
        assertThatThrownBy(() -> committed(c -> Translation.apply(c, "worker-a", false))).isInstanceOf(Rejected.class).hasMessage("TRANSLATION_LEASE_LOST");
        originalContributions();
        assertThat(value("SELECT version FROM cnt_localization WHERE id=?", TARGET)).isZero();
    }
    @Test void newerSourceRevisionSupersedesOldMachineResult() throws Exception {
        capture();
        committed(c -> { Content.parent(c); update(c, "UPDATE cnt_localization SET published_revision_no=2 WHERE id=?", SOURCE); });
        committed(c -> assertThat(Translation.apply(c, "worker-a", false)).isFalse());
        originalContributions();
        assertThat(field("SELECT status FROM trn_job_item WHERE id=?", ITEM)).isEqualTo("SUPERSEDED");
    }
    @Test void manualModeCannotReceiveMachineOutput() throws Exception {
        committed(c -> { Content.parent(c); update(c, "UPDATE cnt_localization SET translation_mode='MANUAL',version=version+1 WHERE id=?", TARGET); });
        capture(); committed(c -> assertThat(Translation.apply(c, "worker-a", false)).isFalse());
        originalContributions();
    }
    @Test void recoveredTaskNeverRefreshesTheCapturedVersionForOldOutput() throws Exception {
        capture();
        committed(c -> {
            Content.target(c, 0);
            update(c, "UPDATE cnt_localization SET draft_title='new-editor-version',version=version+1 WHERE id=?", TARGET);
            update(c, "UPDATE trn_job_item SET worker_id='worker-b' WHERE id=?", ITEM);
        });
        capture(); // resuming the existing item must not bind its old result to version 1
        assertThat(value("SELECT expected_target_version FROM trn_job_item WHERE id=?", ITEM)).isZero();
        committed(c -> assertThat(Translation.apply(c, "worker-b", false)).isFalse());
        assertThat(field("SELECT draft_title FROM cnt_localization WHERE id=?", TARGET)).isEqualTo("new-editor-version");
        assertThat(field("SELECT status FROM trn_job_item WHERE id=?", ITEM)).isEqualTo("SUPERSEDED");
        originalContributions();
    }
    @Test void identicalPolicyIsNoOpButStaleContentVersionStillConflicts() throws Exception {
        committed(c -> Content.policy(c, 0, false));
        assertThat(value("SELECT version FROM cnt_localization WHERE id=?", TARGET)).isZero();
        assertThat(value("SELECT version FROM cnt_content WHERE id=?", CONTENT)).isZero();
        assertThatThrownBy(() -> committed(c -> Content.policy(c, 1, false))).isInstanceOf(Rejected.class).hasMessage("CONTENT_VERSION_CONFLICT");
    }
}
