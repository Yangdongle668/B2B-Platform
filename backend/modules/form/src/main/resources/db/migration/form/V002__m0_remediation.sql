ALTER TABLE frm_submission
  ADD COLUMN download_grant JSON NULL COMMENT '服务端固定下载结果：url/expiresAt/contentId/locale；幂等重放不续期',
  ADD CONSTRAINT ck_sub_download_grant CHECK (
    download_grant IS NULL OR
    (spam_verdict = 'ACCEPT' AND is_test = 0 AND download_media_id IS NOT NULL)
  );
