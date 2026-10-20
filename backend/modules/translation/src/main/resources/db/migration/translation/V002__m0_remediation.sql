ALTER TABLE trn_job_item
  ADD COLUMN expected_target_version INT NULL COMMENT 'CONTENT：外部计算前捕获的目标草稿版本；恢复不刷新',
  ADD CONSTRAINT ck_trn_expected_target_version CHECK (
    expected_target_version IS NULL OR (item_kind = 'CONTENT' AND expected_target_version >= 0)
  );
