CREATE TYPE application_status AS ENUM (
    'SOURCED',
    'IN_PROGRESS',
    'INTERVIEW',
    'HIRED',
    'REJECTED',
    'LAYOFF'
);

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    resume_url TEXT,
    status application_status NOT NULL DEFAULT 'SOURCED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_applications_organization_id ON applications (organization_id);
CREATE INDEX idx_applications_job_id_email ON applications (job_id, email);

CREATE TYPE interview_phase AS ENUM (
    'SCREENING',
    'HIRING_MANAGER',
    'TECHNICAL'
);

CREATE TYPE interview_status AS ENUM (
    'SCHEDULED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TABLE interviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    interviewer_id UUID NOT NULL,
    phase interview_phase NOT NULL,
    status interview_status NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at TIMESTAMPTZ,
    notes TEXT,
    passed BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_application
        FOREIGN KEY (application_id)
        REFERENCES applications (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_interviews_application_id ON interviews (application_id);
