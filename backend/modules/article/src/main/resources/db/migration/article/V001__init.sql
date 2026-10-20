CREATE TABLE art_author (
  id               BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id          BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  name             VARCHAR(100)  NOT NULL COMMENT '姓名（对外署名，所有语言相同，不翻译）；Schema.org Person.name',
  avatar_media_id  BIGINT        NULL COMMENT '头像媒体 ID（mda_media.id，类型 IMAGE）',
  social_links     JSON          NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '社交链接：[{"type": "LINKEDIN", "url": "https://…"}]；Schema.org Person.sameAs',
  sort_order       INT           NOT NULL DEFAULT 0 COMMENT '后台选择列表中的顺序',
  deleted_at       DATETIME(3)   NULL COMMENT '删除时间（软删除）',
  version          INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by       BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by       BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  KEY idx_author_name (site_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者（站点资源：姓名、头像、社交链接）';

CREATE TABLE art_case_study (
  content_id              BIGINT       NOT NULL COMMENT '内容 ID（cnt_content.id，类型 CASE_STUDY），即案例主键',
  site_id                 BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  cover_media_id          BIGINT       NULL COMMENT '封面媒体 ID（mda_media.id，类型 IMAGE）；快照 card.coverMediaId',
  customer_anonymous      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '客户是否匿名：1 = 匿名（不显示客户 Logo，客户名称填写匿名化描述）',
  customer_logo_media_id  BIGINT       NULL COMMENT '客户 Logo 媒体 ID（mda_media.id）；匿名时必须为空',
  customer_country        CHAR(2)      NULL COMMENT '客户所在国家（ISO 3166-1 alpha-2，大写）；前台按语言显示国家名',
  version                 INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by              BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by              BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (content_id),
  CONSTRAINT ck_art_case_anonymous CHECK (customer_anonymous = 0 OR customer_logo_media_id IS NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '案例（与 cnt_content 1:1 的语言无关字段：封面、客户匿名、客户 Logo 与国家）';

CREATE TABLE art_faq (
  content_id  BIGINT       NOT NULL COMMENT '内容 ID（cnt_content.id，类型 FAQ），即 FAQ 主键',
  site_id     BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '默认顺序（越小越靠前；写入快照，供 FAQ 组件与 faq-by-context 数据源排序）',
  version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by  BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'FAQ（不可路由的可复用问答；与 cnt_content 1:1）';

CREATE TABLE art_article (
  content_id                BIGINT       NOT NULL COMMENT '内容 ID（cnt_content.id，类型 ARTICLE），即文章主键',
  site_id                   BIGINT       NOT NULL COMMENT '站点 ID（sys_site.id）',
  author_id                 BIGINT       NULL COMMENT '作者 ID（art_author.id）；空 = 不署名（结构化数据 author 使用 Organization）',
  primary_category_term_id  BIGINT       NULL COMMENT '主栏目 ID（tx_term.id，词汇表 article_category）；冗余 tx_assignment 中的主分类',
  cover_media_id            BIGINT       NULL COMMENT '封面媒体 ID（mda_media.id，类型 IMAGE）；快照 card.coverMediaId',
  version                   INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at                DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by                BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at                DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by                BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (content_id),
  KEY idx_article_author (author_id),
  KEY idx_article_category (site_id, primary_category_term_id),
  CONSTRAINT fk_art_article_author FOREIGN KEY (author_id) REFERENCES art_author (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章（新闻、博客；与 cnt_content 1:1 的语言无关字段：作者、主栏目、封面）';

CREATE TABLE art_author_localization (
  id                  BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id             BIGINT         NOT NULL COMMENT '站点 ID（sys_site.id）',
  author_id           BIGINT         NOT NULL COMMENT '作者 ID（art_author.id）',
  locale              VARCHAR(16)    NOT NULL COMMENT '语言（BCP 47）',
  job_title           VARCHAR(200)   NULL COMMENT '职务（站点资源：中文录入，其他语言由 translation 写入），如“储能系统高级工程师”',
  bio                 VARCHAR(1000)  NULL COMMENT '简介（纯文本；站点资源：中文录入，其他语言由 translation 写入）',
  translation_state   VARCHAR(16)    NOT NULL DEFAULT 'NONE' COMMENT '翻译状态：NONE / QUEUED / TRANSLATED / NEEDS_REVIEW / FAILED / OUTDATED',
  source_fingerprint  CHAR(64)       COLLATE utf8mb4_0900_bin NULL COMMENT '生成本行译文时中文行的指纹（规范化 job_title 与 bio 拼接后的 SHA-256）；中文行为空；与当前中文指纹不同即为过期',
  version             INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统）',
  updated_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by          BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_aul_author_locale (author_id, locale),
  KEY idx_aul_state (site_id, translation_state, locale),
  CONSTRAINT fk_art_aul_author FOREIGN KEY (author_id) REFERENCES art_author (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作者本地化（职务与简介为站点资源，带翻译状态与源指纹）';

CREATE TABLE art_case_study_localization (
  id             BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id        BIGINT         NOT NULL COMMENT '站点 ID（sys_site.id）',
  case_study_id  BIGINT         NOT NULL COMMENT '案例 ID（art_case_study.content_id，即 cnt_content.id）',
  locale         VARCHAR(16)    NOT NULL COMMENT '语言（BCP 47），与 cnt_localization.locale 一一对应',
  title          VARCHAR(300)   NOT NULL COMMENT '标题（页面 H1；同步为 cnt_localization.draft_title；快照 card.title）',
  summary        VARCHAR(1000)  NULL COMMENT '摘要（纯文本；快照 card.summary）',
  customer_name  VARCHAR(200)   NULL COMMENT '客户显示名称；匿名时为匿名化描述，如“德国某储能系统集成商”',
  challenge      JSON           NULL COMMENT '挑战（ProseMirror JSON 富文本）',
  solution       JSON           NULL COMMENT '方案（ProseMirror JSON 富文本）',
  results        JSON           NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '成果指标列表：[{"value": "30%", "label": "运维成本降低"}]，按顺序显示',
  version        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by     BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统，如译文回写）',
  updated_at     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by     BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统，如译文回写）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_csloc_case_locale (case_study_id, locale),
  KEY idx_csloc_title (site_id, locale, title),
  CONSTRAINT fk_art_csloc_case FOREIGN KEY (case_study_id) REFERENCES art_case_study (content_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '案例本地化（内容类草稿：标题、摘要、客户名称、挑战、方案、成果）';

CREATE TABLE art_faq_localization (
  id          BIGINT        NOT NULL COMMENT '主键（TSID）',
  site_id     BIGINT        NOT NULL COMMENT '站点 ID（sys_site.id）',
  faq_id      BIGINT        NOT NULL COMMENT 'FAQ ID（art_faq.content_id，即 cnt_content.id）',
  locale      VARCHAR(16)   NOT NULL COMMENT '语言（BCP 47），与 cnt_localization.locale 一一对应',
  question    VARCHAR(300)  NOT NULL COMMENT '问题（纯文本；同步为 cnt_localization.draft_title；快照 card.title）',
  answer      JSON          NULL COMMENT '答案（ProseMirror JSON 富文本）',
  version     INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by  BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统，如译文回写）',
  updated_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by  BIGINT        NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统，如译文回写）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_floc_faq_locale (faq_id, locale),
  KEY idx_floc_question (site_id, locale, question),
  CONSTRAINT fk_art_floc_faq FOREIGN KEY (faq_id) REFERENCES art_faq (content_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'FAQ 本地化（内容类草稿：问题、答案）';

CREATE TABLE art_article_localization (
  id               BIGINT         NOT NULL COMMENT '主键（TSID）',
  site_id          BIGINT         NOT NULL COMMENT '站点 ID（sys_site.id）',
  article_id       BIGINT         NOT NULL COMMENT '文章 ID（art_article.content_id，即 cnt_content.id）',
  locale           VARCHAR(16)    NOT NULL COMMENT '语言（BCP 47），与 cnt_localization.locale 一一对应',
  title            VARCHAR(300)   NOT NULL COMMENT '标题（页面 H1；同步为 cnt_localization.draft_title；快照 card.title）',
  summary          VARCHAR(1000)  NULL COMMENT '摘要（纯文本；快照 card.summary）',
  body             JSON           NULL COMMENT '正文（ProseMirror JSON，节点白名单；从 H2 开始；内部链接为 content://{id}）',
  reading_minutes  SMALLINT       NOT NULL DEFAULT 0 COMMENT '阅读时长（分钟，保存时按本语言正文自动计算；正文为空时为 0）',
  version          INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
  created_by       BIGINT         NOT NULL DEFAULT 0 COMMENT '创建人用户 ID（0 = 系统，如译文回写）',
  updated_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  updated_by       BIGINT         NOT NULL DEFAULT 0 COMMENT '更新人用户 ID（0 = 系统，如译文回写）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_aloc_article_locale (article_id, locale),
  KEY idx_aloc_title (site_id, locale, title),
  CONSTRAINT fk_art_aloc_article FOREIGN KEY (article_id) REFERENCES art_article (content_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章本地化（内容类草稿：标题、摘要、正文、阅读时长）';
