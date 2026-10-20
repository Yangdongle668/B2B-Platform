CREATE TABLE pg_component_usage (
  localization_id    BIGINT        NOT NULL COMMENT '本地化 ID（cnt_localization.id，已发布）',
  component_type     VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '组件类型（组件定义的 type），如 hero、inquiry-form、global-block',
  component_version  SMALLINT      NOT NULL COMMENT '组件版本（布局中的 componentVersion）',
  ref_key            VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL DEFAULT '' COMMENT '组件引用的 key：inquiry-form 的 formKey、global-block 的区块 key；其他组件为空字符串',
  site_id            BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  content_id         BIGINT        NOT NULL COMMENT '内容 ID（cnt_content.id）',
  content_type       VARCHAR(32)   NOT NULL COMMENT '内容类型：PAGE / PRODUCT / ARTICLE / CASE_STUDY / TERM_PAGE / GLOBAL_BLOCK',
  locale             VARCHAR(16)   NOT NULL COMMENT '语言（BCP 47）',
  revision_no        INT           NOT NULL COMMENT '建立索引所依据的修订号（cnt_revision.revision_no）；事件乱序时只接受更大的修订号',
  usage_count        SMALLINT      NOT NULL DEFAULT 1 COMMENT '该组件（同版本、同 key）在本次发布中出现的次数（含插槽与嵌套 children）',
  created_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立时间（UTC；每次发布整体重建）',
  PRIMARY KEY (localization_id, component_type, component_version, ref_key),
  KEY idx_cu_component (component_type, component_version),
  KEY idx_cu_ref (site_id, component_type, ref_key),
  KEY idx_cu_content (content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组件使用索引（发布态投影：已发布本地化使用的组件、版本与引用 key）';

CREATE TABLE pg_global_block (
  content_id   BIGINT        NOT NULL COMMENT '内容 ID（cnt_content.id，类型 GLOBAL_BLOCK），即区块主键',
  site_id      BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  block_key    VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL COMMENT '区块 key（小写字母、数字、连字符；创建后不可修改；global-block 组件与模板插槽默认值按 key 引用），如 product-cta',
  name         VARCHAR(100)  NOT NULL COMMENT '名称（中文，只在后台显示）',
  description  VARCHAR(500)  NULL COMMENT '说明（后台提示用途与放置位置）',
  version      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by   BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (content_id),
  UNIQUE KEY uk_block_key (site_id, block_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '全局区块（与 cnt_content 1:1：区块 key 与后台名称）';

CREATE TABLE pg_layout (
  id            BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id       BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  content_id    BIGINT        NOT NULL COMMENT '内容 ID（cnt_content.id）',
  locale        VARCHAR(16)   NOT NULL COMMENT '语言（BCP 47），与 cnt_localization.locale 一一对应',
  content_type  VARCHAR(32)   NOT NULL COMMENT '内容类型（冗余 cnt_content.type）：PAGE / PRODUCT / ARTICLE / CASE_STUDY / TERM_PAGE / GLOBAL_BLOCK',
  document      JSON          NOT NULL COMMENT '草稿布局文档（顶层 schemaVersion）：PAGE、GLOBAL_BLOCK 为 {template, sections[]}；模板渲染的类型为 {template, slots{插槽 key: sections[]}}',
  template_key  VARCHAR(64)   COLLATE utf8mb4_0900_bin AS (NULLIF(JSON_UNQUOTE(JSON_EXTRACT(document, '$.template')), 'null')) STORED COMMENT '模板 key（生成列，取自 document.template）；GLOBAL_BLOCK 为空',
  version       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（编辑器每 30 秒自动保存）',
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统，如译文回写）',
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by    BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统，如译文回写）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_layout_content_locale (content_id, locale),
  KEY idx_layout_template (site_id, template_key, content_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '草稿布局文档（各内容类型 × 各语言；PAGE 为完整布局，模板渲染类型为插槽布局）';

CREATE TABLE pg_page (
  content_id         BIGINT        NOT NULL COMMENT '内容 ID（cnt_content.id，类型 PAGE），即页面主键',
  site_id            BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  parent_content_id  BIGINT        NULL COMMENT '父页面的内容 ID（pg_page.content_id）；根页面为空',
  tree_path          VARCHAR(128)  COLLATE utf8mb4_0900_bin NOT NULL COMMENT '物化路径：祖先与自身的内容 ID，如 /11/25/',
  depth              TINYINT       NOT NULL DEFAULT 1 COMMENT '层级深度：根为 1，最大 5',
  sort_order         INT           NOT NULL DEFAULT 0 COMMENT '同级排序（后台页面树）',
  template_key       VARCHAR(64)   COLLATE utf8mb4_0900_bin NOT NULL DEFAULT 'default' COMMENT '模板 key（TemplateProvider 注册），如 default、landing、legal；各语言布局文档的 template 与之一致',
  page_role          VARCHAR(16)   NULL COMMENT '页面角色：HOME（首页）/ IMPRINT（Imprint，正文由站点资料生成）；普通页面为空',
  version            INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at         DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by         BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (content_id),
  UNIQUE KEY uk_page_role (site_id, page_role),
  KEY idx_page_parent (parent_content_id, sort_order),
  KEY idx_page_path (site_id, tree_path),
  KEY idx_page_template (site_id, template_key),
  CONSTRAINT fk_pg_page_parent FOREIGN KEY (parent_content_id) REFERENCES pg_page (content_id),
  CONSTRAINT ck_pg_page_depth CHECK ((parent_content_id IS NULL AND depth = 1) OR (parent_content_id IS NOT NULL AND depth BETWEEN 2 AND 5)),
  CONSTRAINT ck_pg_page_parent CHECK (parent_content_id IS NULL OR parent_content_id <> content_id),
  CONSTRAINT ck_pg_page_role CHECK (page_role IS NULL OR (page_role = 'HOME' AND parent_content_id IS NULL) OR (page_role = 'IMPRINT' AND template_key = 'legal'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '页面（与 cnt_content 1:1 的语言无关字段：父页面、模板、页面角色）';
