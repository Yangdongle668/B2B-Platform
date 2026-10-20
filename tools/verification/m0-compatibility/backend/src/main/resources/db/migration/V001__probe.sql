CREATE TABLE probe_document (id BIGINT PRIMARY KEY, site_id BIGINT NOT NULL, title VARCHAR(80) NOT NULL);
CREATE TABLE probe_receipt (id BIGINT PRIMARY KEY, site_id BIGINT NOT NULL);
INSERT INTO probe_document VALUES (1, 1, 'site A'), (2, 2, 'site B');
