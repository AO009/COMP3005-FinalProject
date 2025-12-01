-- COMP 3005 Final Project - DML - Ahmed Omer


SET search_path TO public;

-- Clean existing data and reset SERIAL counters
TRUNCATE TABLE
    Registers,
    IndividualAvailability,
    FitnessGoals,
    HealthMetric,
    FitnessClass,
    Room,
    AdministrationStaff,
    Trainer,
    Member
RESTART IDENTITY CASCADE;


-- MEMBERS Table


INSERT INTO Member (fname, lname, date_of_birth, gender, email, phone_number) VALUES
('Anthony', 'Soprano Jr.', '1989-07-15', 'Male', 'aj.soprano@example.com', '613-555-0001'),
('Meadow', 'Soprano', '1984-09-13','Female', 'meadow.soprano@example.com', '613-555-0002'),
('Adriana', 'La Cerva', '1972-11-12', 'Female', 'adriana.lacerva@example.com', '613-555-0003'),
('Furio','Giunta', '1965-03-02', 'Male', 'furio.giunta@example.com', '613-555-0004'),
('Corrado', 'Soprano',  '1930-04-29', 'Male',  'uncle.junior@example.com', '613-555-0005');


-- TRAINERS Table


INSERT INTO Trainer (fname, lname, date_of_birth, gender, email, phone_number) VALUES
('Tony','Soprano', '1959-08-22', 'Male', 'tony.soprano@example.com', '343-555-1001'),
('Silvio', 'Dante','1957-05-05', 'Male', 'silvio.dante@example.com', '343-555-1002'),
('Paulie', 'Gualtieri', '1942-07-04', 'Male', 'paulie.gualtieri@example.com', '343-555-1003'),
('Christopher','Moltisanti','1969-09-01', 'Male', 'chris.moltisanti@example.com', '343-555-1004'),
('Carmela', 'Soprano', '1960-03-23', 'Female', 'carmela.soprano@example.com', '343-555-1005');


-- ADMINISTRATION STAFF Table


INSERT INTO AdministrationStaff (fname, lname, date_of_birth, gender, email, phone_number) VALUES
('Artie', 'Bucco', '1960-01-10', 'Male', 'artie.bucco@example.com', '613-555-2001'),
('Jennifer', 'Melfi', '1955-05-20', 'Female', 'jennifer.melfi@example.com', '613-555-2002'),
('Hesh', 'Rabkin', '1940-12-01', 'Male', 'hesh.rabkin@example.com', '613-555-2003'),
('Eugene', 'Pontecorvo', '1965-06-06', 'Male', 'eugene.ponte@example.com', '613-555-2004'),
('Charmaine', 'Bucco', '1962-02-18', 'Female', 'charmaine.bucco@example.com', '613-555-2005');


-- ROOMS Table

INSERT INTO Room (location) VALUES
('Bada Bing Studio'),
('Satriales Weight Room'),
('Back Room Boxing Gym'),
('Pine Barrens Cardio'),
('Vesuvio Spin Loft');


-- INDIVIDUAL AVAILABILITIES Table

INSERT INTO IndividualAvailability (trainer_id, start_hour, end_hour, date) VALUES
(1, '09:00', '12:00', '2025-12-01'), 
(2, '10:00', '13:00', '2025-12-02'), 
(3, '08:00', '11:00', '2025-12-03'), 
(4, '16:00', '19:00', '2025-12-04'),
(5, '07:00', '10:00', '2025-12-05');


-- FITNESS CLASS Table


INSERT INTO FitnessClass (capacity, date, start_hour, end_hour, room_id, trainer_id, admin_id) VALUES
(20, '2025-12-01', '09:30', '10:30', 1, 1, 1), 
(15, '2025-12-02', '10:30', '12:00', 2, 2, 2), 
(12, '2025-12-03', '08:30', '10:00', 3, 3, 3), 
(25, '2025-12-04', '16:30', '18:00', 4, 4, 4), 
(18, '2025-12-05', '07:30', '09:00', 5, 5, 5); 


-- HEALTH METRICS Table

INSERT INTO HealthMetric (member_id, timestamp, height, weight, vo2_max, body_fat_percentage) VALUES
(1, '2025-12-01 08:00:00', 175.00, 82.0, 38.5, 24.0), 
(2, '2025-12-01 08:05:00', 168.00, 60.0, 42.0, 20.0), 
(3, '2025-12-01 08:10:00', 165.00, 58.0, 36.0, 23.0), 
(4, '2025-12-01 08:15:00', 180.00, 78.0, 45.0, 18.0),
(5, '2025-12-01 08:20:00', 170.00, 85.0, 30.0, 28.0);


-- FITNESS GOALS Table


INSERT INTO FitnessGoals (member_id, timestamp, weight_goal, vo2_max_goal, body_fat_percentage_goal) VALUES
(1, '2025-12-01 09:00:00', 78.0, 40.0, 22.0),
(2, '2025-12-01 09:00:00', 58.0, 44.0, 18.0),
(3, '2025-12-01 09:00:00', 55.0, 38.0, 21.0),
(4, '2025-12-01 09:00:00', 76.0, 47.0, 17.0),
(5, '2025-12-01 09:00:00', 82.0, 32.0, 26.0);


-- REGISTERS Table


INSERT INTO Registers (member_id, fitness_class_id) VALUES
(1, 1), 
(2, 2),  
(3, 3),  
(4, 4),  
(5, 5);  
