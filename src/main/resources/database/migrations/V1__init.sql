CREATE TYPE application_status AS ENUM ('SOURCED', 'IN_PROGRESS', 'INTERVIEW', 'HIRED', 'REJECTED','LAYOFF');

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    resume_url TEXT,
    status application_status NOT NULL DEFAULT 'SOURCED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE applications
    ADD CONSTRAINT unique_candidate_job
        UNIQUE (job_id, candidate_id);