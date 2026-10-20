ALTER TABLE mda_media
  ADD COLUMN gated_form_key VARCHAR(64) COLLATE utf8mb4_0900_bin NULL COMMENT '留资门控表单key；空表示无门控',
  ADD COLUMN download_gate_version BIGINT NOT NULL DEFAULT 0 COMMENT '门控及删除/恢复变更代次，不回退',
  ADD COLUMN purge_job_id BIGINT NULL COMMENT '已提交物理清理任务ID；非空禁止恢复，逻辑引用core任务',
  ADD CONSTRAINT ck_mda_gate_version CHECK (download_gate_version >= 0);
