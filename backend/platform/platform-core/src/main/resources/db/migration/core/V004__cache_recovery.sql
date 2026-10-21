CREATE TABLE sys_cache_recovery (
  id TINYINT NOT NULL PRIMARY KEY,
  boot VARCHAR(64) NOT NULL DEFAULT '',
  epoch CHAR(36) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
  started_at DATETIME(6) NULL
);
INSERT INTO sys_cache_recovery(id) VALUES(1);

CREATE TABLE sys_cache_recovery_publication (
  epoch CHAR(36) NOT NULL,
  publication_id VARCHAR(36) NOT NULL,
  PRIMARY KEY(epoch, publication_id)
);
