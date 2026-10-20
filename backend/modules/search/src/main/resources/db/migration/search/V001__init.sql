CREATE TABLE sch_build (
  id                       BIGINT       NOT NULL COMMENT 'buildId/TSID；每次尝试全新',
  site_id                  BIGINT       NOT NULL,
  locale                   VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  job_id                   BIGINT       NOT NULL COMMENT '经JobApi创建，逻辑引用',
  physical_index           VARCHAR(200) COLLATE utf8mb4_0900_bin NOT NULL,
  base_index               VARCHAR(200) COLLATE utf8mb4_0900_bin NULL COMMENT '复制基础代次；存活构建引用时禁止GC',
  expected_active_version  BIGINT       NOT NULL,
  build_fence              BIGINT       NOT NULL,
  start_seq                BIGINT       NOT NULL COMMENT '全量H0；复制从基础代次applied_seq起',
  applied_seq              BIGINT       NOT NULL,
  status                   VARCHAR(16)  NOT NULL DEFAULT 'BUILDING',
  last_engine_task_id      BIGINT       NULL COMMENT '仅单执行流顺序发出并逐个await',
  heartbeat_at             DATETIME(3)  NULL COMMENT '调度提示；不是发布资格依据',
  created_at               DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by               BIGINT       NOT NULL DEFAULT 0,
  updated_at               DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by               BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_physical (site_id, physical_index),
  KEY idx_search_build (site_id, locale, status),
  CONSTRAINT ck_search_build_state CHECK (status IN ('BUILDING','READY','ACTIVE','RETIRED','ABANDONED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索不可变索引代次';

CREATE TABLE sch_change (
  id                BIGINT       NOT NULL COMMENT 'TSID',
  site_id           BIGINT       NOT NULL,
  locale            VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  sequence_no       BIGINT       NOT NULL COMMENT '同语言提交序号',
  localization_ids  JSON         NOT NULL COMMENT '完整受影响ID集合；全量刷新时为空数组',
  full_refresh      TINYINT(1)   NOT NULL DEFAULT 0,
  created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by        BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_change (site_id, locale, sequence_no),
  CONSTRAINT ck_search_full CHECK (full_refresh IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索事务变更日志';

CREATE TABLE sch_index_state (
  id              BIGINT       NOT NULL COMMENT 'TSID',
  site_id         BIGINT       NOT NULL,
  locale          VARCHAR(16) COLLATE utf8mb4_0900_bin NOT NULL,
  change_seq      BIGINT       NOT NULL DEFAULT 0 COMMENT '源事务顺序，行锁保护',
  applied_seq     BIGINT       NOT NULL DEFAULT 0 COMMENT '活跃代次已追平水位',
  active_index    VARCHAR(200) COLLATE utf8mb4_0900_bin NULL COMMENT '不可变物理索引名',
  active_version  BIGINT       NOT NULL DEFAULT 0 COMMENT '指针CAS版本',
  build_fence     BIGINT       NOT NULL DEFAULT 0 COMMENT '构建认领代次',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by      BIGINT       NOT NULL DEFAULT 0,
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by      BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_search_state (site_id, locale),
  CONSTRAINT ck_search_seq CHECK (change_seq >= applied_seq AND applied_seq >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索活跃指针与事务水位';
