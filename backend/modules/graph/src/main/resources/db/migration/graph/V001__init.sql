CREATE TABLE grh_config (
  id            BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id       BIGINT         NOT NULL COMMENT '站点',
  content_type  VARCHAR(32)    NOT NULL COMMENT '源内容类型：PAGE / PRODUCT / ARTICLE / CASE_STUDY / TERM_PAGE（可路由类型）',
  weights       JSON           NOT NULL COMMENT '评分权重：{"schemaVersion":1,"primaryCategory":30,"category":15,"application":{"each":25,"max":50},…,"typeAffinity":{"PRODUCT":10},"freshness":{"max":10,"days":365}}',
  threshold     DECIMAL(7, 2)  NOT NULL DEFAULT 30.00 COMMENT '阈值：得分低于此值的候选不保留（默认 30）',
  top_k         INT            NOT NULL DEFAULT 20 COMMENT '每个目标类型保留的自动关系条数（默认 20）',
  auto_enabled  TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '是否为该类型的源内容计算自动关系：0 = 只显示人工关系',
  version       INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at    DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by    BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人',
  updated_at    DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by    BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cfg_type (site_id, content_type),
  CONSTRAINT ck_grh_cfg_range CHECK (top_k BETWEEN 1 AND 50 AND threshold >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱评分配置（站点 × 源内容类型；没有行时使用内置默认值）';

CREATE TABLE grh_content_mode (
  id          BIGINT       NOT NULL COMMENT '主键（TSID）',
  site_id     BIGINT       NOT NULL COMMENT '站点',
  content_id  BIGINT       NOT NULL COMMENT '内容（cnt_content.id）',
  mode        VARCHAR(16)  NOT NULL COMMENT '相关推荐模式：MANUAL（只显示人工关系，不计算自身的自动关系）/ DISABLED（不显示相关内容）；默认的 AUTO 不保存行',
  version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人',
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cmode_content (content_id),
  CONSTRAINT ck_grh_cmode_mode CHECK (mode IN ('MANUAL', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内容的相关推荐模式（只保存非默认的 MANUAL / DISABLED）';

CREATE TABLE grh_dirty (
  id             BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id        BIGINT        NOT NULL COMMENT '站点',
  content_id     BIGINT        NOT NULL COMMENT '待重算的源内容（cnt_content.id）',
  reason         VARCHAR(24)   NOT NULL COMMENT '最近一次触发原因：PUBLISHED / UNPUBLISHED / TRASHED / RESTORED / TERM_CHANGED / MANUAL_CHANGED / MODE_CHANGED',
  trigger_count  INT           NOT NULL DEFAULT 1 COMMENT '去抖期间合并的触发次数',
  due_at         DATETIME(3)   NOT NULL COMMENT '最早处理时间：最近一次触发 + 30 秒，且不晚于 created_at + 5 分钟（避免批量导入时一直推迟）',
  locked_until   DATETIME(3)   NULL COMMENT '处理租约到期时间：取件时设为当前时间 + 5 分钟，防止多实例重复计算',
  attempts       INT           NOT NULL DEFAULT 0 COMMENT '失败次数（超过 5 次告警，行保留到下次全量重建后删除）',
  last_error     VARCHAR(500)  NULL COMMENT '最近一次失败原因',
  version        INT           NOT NULL DEFAULT 0 COMMENT '每次合并触发 + 1；处理完成后按版本条件删除，处理期间的新触发不会丢失',
  created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次触发时间',
  updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dirty_content (site_id, content_id),
  KEY idx_dirty_due (due_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱增量重算队列（同一内容的触发合并去抖 30 秒）';

CREATE TABLE grh_relation (
  id                   BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id              BIGINT         NOT NULL COMMENT '站点',
  source_content_id    BIGINT         NOT NULL COMMENT '源内容（cnt_content.id）',
  target_content_id    BIGINT         NOT NULL COMMENT '目标内容（cnt_content.id）',
  target_content_type  VARCHAR(32)    NOT NULL COMMENT '目标内容类型（冗余 cnt_content.type）：PAGE / PRODUCT / ARTICLE / CASE_STUDY / TERM_PAGE；自动关系按目标类型各保留前 top_k 条',
  relation_type        VARCHAR(40)    COLLATE utf8mb4_0900_bin NOT NULL COMMENT '关系类型（grh_relation_type.type_key）：RELATED / RECOMMENDED / USES / USED_IN / CASE_OF / HAS_CASE / ACCESSORY_OF / HAS_ACCESSORY / REPLACED_BY / REPLACES / EXCLUDED，或模块注册的其他类型',
  origin               VARCHAR(10)    NOT NULL COMMENT '来源：MANUAL（人工，始终置顶）/ AUTO（规则评分）',
  score                DECIMAL(7, 2)  NULL COMMENT '得分（AUTO）；MANUAL 为空',
  rank_no              INT            NOT NULL DEFAULT 0 COMMENT '排名：MANUAL 为同一源内容内的人工排序（从 1 开始）；AUTO 为同一目标类型内按得分的名次（从 1 开始）',
  pinned               TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '固定（只用于 AUTO）：1 = 编辑固定了这条自动推荐，重算时保留并更新得分与原因；MANUAL 恒为 0',
  reason               JSON           NULL COMMENT '得分原因（AUTO）：{"primaryCategory":30,"application":{"score":25,"terms":["可穿戴设备"]},"tag":{"score":10,"terms":["低温"]}}',
  computed_at          DATETIME(3)    NULL COMMENT '计算时间（AUTO）',
  created_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  created_by           BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人（0 = 系统，即自动关系）',
  updated_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  updated_by           BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grh_relation (site_id, source_content_id, target_content_id, relation_type),
  KEY idx_grh_relation_source (site_id, source_content_id, origin, rank_no),
  KEY idx_grh_relation_target (site_id, target_content_id),
  CONSTRAINT ck_grh_rel_self CHECK (source_content_id <> target_content_id),
  CONSTRAINT ck_grh_rel_origin CHECK ((origin = 'AUTO' AND score IS NOT NULL AND computed_at IS NOT NULL) OR (origin = 'MANUAL' AND score IS NULL AND pinned = 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内容关系（建立在语言无关的内容上；人工关系与规则评分的自动关系）';

CREATE TABLE grh_relation_set (
  id                 BIGINT       NOT NULL COMMENT 'TSID',
  site_id            BIGINT       NOT NULL COMMENT '站点ID',
  source_content_id  BIGINT       NOT NULL COMMENT '源内容ID，逻辑引用',
  version            BIGINT       NOT NULL DEFAULT 0 COMMENT '集合版本；空集合也递增和保留',
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by         BIGINT       NOT NULL DEFAULT 0,
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by         BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_relation_set (site_id, source_content_id),
  CONSTRAINT ck_relation_set_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容关系集合的共同版本';

CREATE TABLE grh_relation_type (
  id                    BIGINT       NOT NULL COMMENT '主键（TSID）',
  type_key              VARCHAR(40)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '类型 key（大写字母与下划线，全局唯一），如 RELATED、USES；创建后不可改名',
  module_id             VARCHAR(32)  NOT NULL COMMENT '注册模块 ID（RelationTypeProvider 的实现模块）：graph / product 等',
  label                 VARCHAR(50)  NOT NULL COMMENT '名称（中文，后台显示），如“相关”“配件”',
  symmetric             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否对称：1 = A→B 即 B→A，人工关系成对写入同一类型',
  inverse_key           VARCHAR(40)  COLLATE utf8mb4_0900_bin NULL COMMENT '反向类型 key（互逆关系成对写入，如 USES ↔ USED_IN）；对称类型与单向类型为空',
  allowed_source_types  JSON         NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '允许的源内容类型数组，如 ["CASE_STUDY","ARTICLE"]；空数组表示全部可路由类型',
  allowed_target_types  JSON         NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '允许的目标内容类型数组，如 ["PRODUCT"]；空数组表示全部可路由类型',
  auto_allowed          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否可由规则评分生成自动关系（V1 只有 RELATED 为 1；其他类型只能人工建立）',
  sort_order            INT          NOT NULL DEFAULT 0 COMMENT '后台类型选择器中的顺序（小的在前）',
  available             TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '注册模块是否仍提供该类型：0 = 代码中已不存在，已有关系保留并可读取，不能新建',
  created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（首次同步）',
  updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（最近一次同步变化）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rtype_key (type_key),
  CONSTRAINT ck_grh_rtype_inverse CHECK (symmetric = 0 OR inverse_key IS NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '关系类型（全局表，由 RelationTypeProvider 同步；列入站点过滤豁免清单）';
