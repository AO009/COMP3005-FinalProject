# Ahmed Omer Comp 3005 Final Project - 101270274 

# Fitness Club Management System – COMP 3005 Final Project, Solo

# Video Link: https://youtu.be/Tf0uiXxdWZ4

## 1. Overview

This project is a Fitness Club System implemented with:

- PostgreSQL as the relational database
- A Java CLI application as the front-end
- This project includes:
      - ER diagram, Schema, Normalized Schema, triggers, views, and an index

The system supports three user roles and implements the following operational requirements:

# Member
    - User Registration: Through the showDashboard function
    - Profile Management: Through the updatePersonalDetails, updateFitnessGoals, addHealthMetric & updateFitnessGoals functions
    - Health History: Through the addHealthMetric function
    - Group Class Registration: Through the registerForClass function
    - Dashboard: Through the showDashboard function
    
# Trainer
    - Set Availability: Through the setAvailability function
    - Schedule View: Through the viewSchedule function
  
# Administration Staff
    - Room Booking: Through the createClass function
    - Class Management: Through the createClass, cancelClass & updateClass functions
    

The project demonstrates:

- ER → relational mapping
- 1NF / 2NF / 3NF normalization
- Business rules enforced by triggers
- A reusable view for member schedules
- A performance-related index for schedule queries
- A Java CLI that uses JDBC to interact with the database

# To Build and Run: mvn -DskipTests exec:java -Dexec.mainClass="app.Main"


## 2. Repository / Folder Structure

The project follows the required structure:

       /project-root
        /sql
          - DDL.sql # All CREATE TABLE, triggers, view, index
          - DML.sql # Sample data (5+ records per table)

         /app
          - pom.xml
          - src/
            - main/
              - java/

         /docs
          - COMP 3005 Final Project Ahmed Omer ER + Schema + Assumptions.pdf  # ER diagram + Schema mapping + assumptions
          - Normalization_ COMP 3005 Final Project.pdf # Normalization
          - README.md    # video link + run instructions
