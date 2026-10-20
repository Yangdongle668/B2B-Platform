CREATE TABLE sys_processed_event (
  consumer VARCHAR(96) COLLATE utf8mb4_0900_bin NOT NULL,
  event_id VARCHAR(20) COLLATE utf8mb4_0900_bin NOT NULL,
  site_id BIGINT NULL,
  processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (consumer, event_id),
  KEY idx_pev_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消费者事务幂等标记';
