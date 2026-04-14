CREATE TABLE rate_limit_bucket (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    bucket_date DATE NOT NULL,
    principal VARCHAR(128) NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    limit_value INTEGER NOT NULL
);

CREATE UNIQUE INDEX uk_rate_limit_bucket_date_principal
    ON rate_limit_bucket (bucket_date, principal);

CREATE TABLE job_lease (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    job_name VARCHAR(120) NOT NULL,
    owner_id VARCHAR(120) NOT NULL,
    lease_until TIMESTAMP NOT NULL,
    last_acquired_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_job_lease_job_name
    ON job_lease (job_name);
