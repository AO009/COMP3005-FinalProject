-- COMP 3005 Final Project - DDL - Ahmed Omer

-- Schema setup
CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;



DROP VIEW IF EXISTS MemberClassSchedule;


DROP TABLE IF EXISTS Registers CASCADE;
DROP TABLE IF EXISTS IndividualAvailability CASCADE;
DROP TABLE IF EXISTS FitnessGoals CASCADE;
DROP TABLE IF EXISTS HealthMetric CASCADE;
DROP TABLE IF EXISTS FitnessClass CASCADE;
DROP TABLE IF EXISTS Room CASCADE;
DROP TABLE IF EXISTS AdministrationStaff CASCADE;
DROP TABLE IF EXISTS Trainer CASCADE;
DROP TABLE IF EXISTS Member CASCADE;


DROP FUNCTION IF EXISTS check_member_registration_conflict();
DROP FUNCTION IF EXISTS check_fitness_class_constraints();
DROP FUNCTION IF EXISTS check_individual_availability_overlap();


DROP INDEX IF EXISTS idx_fitnessclass_date_room;



CREATE TABLE Member (
    id SERIAL PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    email VARCHAR(255), 
    phone_number VARCHAR(30) 
    
);

CREATE TABLE Trainer (
    id SERIAL PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    email VARCHAR(255),
    phone_number VARCHAR(30)
    
);

CREATE TABLE AdministrationStaff (
    id SERIAL PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    email VARCHAR(255),
    phone_number VARCHAR(30)

);

CREATE TABLE Room (
    id SERIAL PRIMARY KEY,
    location VARCHAR(100) NOT NULL
);



CREATE TABLE FitnessClass (
    id  SERIAL PRIMARY KEY,
    capacity INT NOT NULL,
    date DATE NOT NULL,
    start_hour TIME NOT NULL,
    end_hour TIME NOT NULL,
    room_id INT NOT NULL,
    trainer_id INT NOT NULL,
    admin_id INT NOT NULL,
    FOREIGN KEY (room_id)    REFERENCES Room(id),
    FOREIGN KEY (trainer_id) REFERENCES Trainer(id),
    FOREIGN KEY (admin_id)   REFERENCES AdministrationStaff(id)
);


CREATE TABLE HealthMetric (
    member_id INT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    height NUMERIC(5,2),
    weight NUMERIC(5,2),
    vo2_max NUMERIC(5,2),
    body_fat_percentage NUMERIC(5,2),
    PRIMARY KEY (member_id, timestamp),
    FOREIGN KEY (member_id) REFERENCES Member(id) ON DELETE CASCADE
);

CREATE TABLE FitnessGoals (
    member_id INT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    weight_goal NUMERIC(5,2),
    vo2_max_goal NUMERIC(5,2),
    body_fat_percentage_goal NUMERIC(5,2),
    PRIMARY KEY (member_id, timestamp),
    FOREIGN KEY (member_id) REFERENCES Member(id) ON DELETE CASCADE
);


CREATE TABLE IndividualAvailability (
    trainer_id INT NOT NULL,
    start_hour TIME NOT NULL,
    end_hour TIME NOT NULL,
    date DATE NOT NULL,
    PRIMARY KEY (trainer_id, start_hour, end_hour, date),
    FOREIGN KEY (trainer_id) REFERENCES Trainer(id) ON DELETE CASCADE
);


CREATE TABLE Registers (
    member_id INT NOT NULL,
    fitness_class_id INT NOT NULL,
    PRIMARY KEY (member_id, fitness_class_id),
    FOREIGN KEY (member_id) REFERENCES Member(id) ON DELETE CASCADE,
    FOREIGN KEY (fitness_class_id) REFERENCES FitnessClass(id) ON DELETE CASCADE
);


-- View, shows all member registrations (includes member info, trainer info, class info, room info)

CREATE VIEW MemberClassSchedule AS
select m.id AS member_id, m.fname AS member_fname, m.lname AS member_lname, fc.id AS fitness_class_id, fc.date, fc.start_hour, fc.end_hour, 
r.location AS room_location, t.fname AS trainer_fname, t.lname AS trainer_lname
FROM Registers regs
JOIN Member m ON regs.member_id = m.id
JOIN FitnessClass fc ON regs.fitness_class_id = fc.id
JOIN Room r ON fc.room_id  = r.id
JOIN Trainer t ON fc.trainer_id  = t.id;



-- Triggers

-- Prevents overlapping availability for the same trainer and date

CREATE OR REPLACE FUNCTION check_individual_availability_overlap()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM IndividualAvailability ia
        WHERE ia.trainer_id = NEW.trainer_id
          AND ia.date = NEW.date
          -- overlap: NOT (new ends before existing starts OR new starts after existing ends)
          AND NOT (NEW.end_hour <= ia.start_hour OR NEW.start_hour >= ia.end_hour)
    ) THEN
        RAISE EXCEPTION
            'Trainer % already has availability that overlaps with % to % on %',
            NEW.trainer_id, NEW.start_hour, NEW.end_hour, NEW.date;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_check_individual_availability_overlap
BEFORE INSERT OR UPDATE ON IndividualAvailability
FOR EACH ROW
EXECUTE FUNCTION check_individual_availability_overlap();


-- Another trigger to ensure fitness classes work within system constraints (Trainer has availability, trainer & room are not already booked for the time slot)

CREATE OR REPLACE FUNCTION check_fitness_class_constraints()
RETURNS TRIGGER AS $$
BEGIN
    -- Trainer must be available for the entire class window
    IF NOT EXISTS (
        SELECT 1
        FROM IndividualAvailability ia
        WHERE ia.trainer_id = NEW.trainer_id
          AND ia.date = NEW.date
          AND ia.start_hour <= NEW.start_hour
          AND ia.end_hour >= NEW.end_hour
    ) THEN
        RAISE EXCEPTION
            'Trainer % is not available from % to % on %', NEW.trainer_id, NEW.start_hour, NEW.end_hour, NEW.date;
    END IF;

    -- Room is not already booked during this time slot
    IF EXISTS (
        SELECT 1
        FROM FitnessClass fc
        WHERE fc.room_id = NEW.room_id
          AND fc.date = NEW.date
          AND fc.id <> COALESCE(NEW.id, -1) -- ignore the same fitness class for update and pass on inserts
          AND NOT (NEW.end_hour <= fc.start_hour OR NEW.start_hour >= fc.end_hour)
    ) THEN
        RAISE EXCEPTION
            'Room % is already booked for an overlapping class on %', NEW.room_id, NEW.date;
    END IF;

    -- Trainer is not already booked during this time slot
    IF EXISTS (
        SELECT 1
        FROM FitnessClass fc
        WHERE fc.trainer_id = NEW.trainer_id
          AND fc.date = NEW.date
          AND fc.id <> COALESCE(NEW.id, -1) -- ignore the same fitness class for update and pass on inserts
          AND NOT (NEW.end_hour <= fc.start_hour OR NEW.start_hour >= fc.end_hour)
    ) THEN
        RAISE EXCEPTION 'Trainer % is already assigned to another overlapping class on %', NEW.trainer_id, NEW.date;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_fitness_class_constraints
BEFORE INSERT OR UPDATE ON FitnessClass
FOR EACH ROW
EXECUTE FUNCTION check_fitness_class_constraints();


-- Final Trigger to prevent a member from registering for an overlapping class with his scheduele 

CREATE OR REPLACE FUNCTION check_member_registration_conflict()
RETURNS TRIGGER AS $$
DECLARE
    class_date DATE;
    class_start TIME;
    class_end TIME;
BEGIN
    -- Get the date/time of the class being registered for
    SELECT date, start_hour, end_hour
    INTO class_date, class_start, class_end
    FROM FitnessClass
    WHERE id = NEW.fitness_class_id;

    -- Check if this member already has an overlapping class on that date
    IF EXISTS (
        SELECT 1
        FROM Registers r
        JOIN FitnessClass fc ON r.fitness_class_id = fc.id
        WHERE r.member_id = NEW.member_id
          AND fc.date = class_date
          AND r.fitness_class_id <> NEW.fitness_class_id
          AND NOT (class_end <= fc.start_hour OR class_start >= fc.end_hour)
    ) THEN
        RAISE EXCEPTION
            'Member % is already registered in an overlapping class on %', NEW.member_id, class_date;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_member_registration_conflict
BEFORE INSERT OR UPDATE ON Registers
FOR EACH ROW
EXECUTE FUNCTION check_member_registration_conflict();


-- Index

CREATE INDEX idx_fitnessclass_date_room
ON FitnessClass(date, room_id);

