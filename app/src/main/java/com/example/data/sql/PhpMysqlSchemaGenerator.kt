package com.example.data.sql

object PhpMysqlSchemaGenerator {

    fun generateFullMySqlSchema(): String {
        return """
-- ============================================================================
-- Enterprise Workforce Attendance & Leave Management Database Schema
-- Database: workforce_hr_db
-- Compatibility: MySQL 5.7+ / MySQL 8.0+ / MariaDB 10.3+ / Percona Server
-- Character Set: utf8mb4 / Collation: utf8mb4_unicode_ci
-- Engine: InnoDB with strict Foreign Key Referential Integrity & Cascades
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `workforce_hr_db` 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `workforce_hr_db`;

-- Drop existing views if re-running
DROP VIEW IF EXISTS `vw_employee_leave_balances`;
DROP VIEW IF EXISTS `vw_daily_attendance_summary`;
DROP VIEW IF EXISTS `vw_monthly_working_hours`;

-- ----------------------------------------------------------------------------
-- 1. Table: departments
-- Organizational structure & business units
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `departments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_code` VARCHAR(20) NOT NULL UNIQUE,
  `dept_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. Table: designations
-- Corporate job titles mapped to departments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `designations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `department_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `grade_level` VARCHAR(20) DEFAULT 'L1',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_designation_dept` (`department_id`),
  CONSTRAINT `fk_designation_department` FOREIGN KEY (`department_id`) 
    REFERENCES `departments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Table: shifts
-- Shift definitions: Morning, Evening, Night, Flexible, Grace & Break rules
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shifts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `shift_name` VARCHAR(100) NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `grace_period_minutes` INT UNSIGNED NOT NULL DEFAULT 15,
  `break_duration_minutes` INT UNSIGNED NOT NULL DEFAULT 45,
  `is_flexible` TINYINT(1) NOT NULL DEFAULT 0,
  `weekly_off_days` VARCHAR(100) NOT NULL DEFAULT 'Saturday,Sunday',
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Table: employees
-- Central directory with IDs, mobile, email, addresses, devices, status
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employees` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `emp_code` VARCHAR(30) NOT NULL UNIQUE,
  `full_name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `mobile` VARCHAR(25) NOT NULL,
  `address` TEXT NOT NULL,
  `department_id` BIGINT UNSIGNED DEFAULT NULL,
  `designation_id` BIGINT UNSIGNED DEFAULT NULL,
  `department` VARCHAR(100) NOT NULL,
  `designation` VARCHAR(100) NOT NULL,
  `joining_date` DATE NOT NULL,
  `role` ENUM('ADMIN', 'MANAGER', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `shift_id` BIGINT UNSIGNED NOT NULL DEFAULT 1,
  `registered_device_id` VARCHAR(100) DEFAULT NULL,
  `registered_device_model` VARCHAR(100) DEFAULT NULL,
  `emergency_contact` VARCHAR(30) DEFAULT NULL,
  `avatar_url` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_emp_code` (`emp_code`),
  INDEX `idx_department` (`department`),
  INDEX `idx_is_active` (`is_active`),
  INDEX `idx_emp_device` (`registered_device_id`),
  CONSTRAINT `fk_employee_shift` FOREIGN KEY (`shift_id`) 
    REFERENCES `shifts` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_employee_department` FOREIGN KEY (`department_id`) 
    REFERENCES `departments` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_employee_designation` FOREIGN KEY (`designation_id`) 
    REFERENCES `designations` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Table: employee_documents
-- Stored documents (Aadhaar, Passport, Degree, Offer Letter)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employee_documents` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT UNSIGNED NOT NULL,
  `document_type` VARCHAR(80) NOT NULL,
  `document_name` VARCHAR(200) NOT NULL,
  `file_url` VARCHAR(255) NOT NULL,
  `uploaded_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_doc_emp` (`employee_id`),
  CONSTRAINT `fk_docs_employee` FOREIGN KEY (`employee_id`) 
    REFERENCES `employees` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. Table: attendance
-- Live punches, GPS geofencing, selfie confidence, fake GPS flag, hours, OT
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT UNSIGNED NOT NULL,
  `date` DATE NOT NULL,
  `check_in_time` TIME DEFAULT NULL,
  `check_out_time` TIME DEFAULT NULL,
  `check_in_timestamp` BIGINT UNSIGNED DEFAULT NULL,
  `check_out_timestamp` BIGINT UNSIGNED DEFAULT NULL,
  `working_minutes` INT UNSIGNED NOT NULL DEFAULT 0,
  `late_minutes` INT UNSIGNED NOT NULL DEFAULT 0,
  `early_exit_minutes` INT UNSIGNED NOT NULL DEFAULT 0,
  `overtime_minutes` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('PRESENT', 'LATE', 'HALF_DAY', 'ABSENT', 'ON_LEAVE', 'EARLY_EXIT') NOT NULL DEFAULT 'PRESENT',
  `latitude` DECIMAL(10, 8) DEFAULT NULL,
  `longitude` DECIMAL(11, 8) DEFAULT NULL,
  `location_address` VARCHAR(255) DEFAULT NULL,
  `is_geofenced` TINYINT(1) NOT NULL DEFAULT 1,
  `distance_meters` FLOAT DEFAULT 0,
  `is_face_verified` TINYINT(1) NOT NULL DEFAULT 1,
  `face_match_confidence` FLOAT DEFAULT 98.0,
  `selfie_url` VARCHAR(255) DEFAULT NULL,
  `is_mock_gps_detected` TINYINT(1) NOT NULL DEFAULT 0,
  `device_id` VARCHAR(100) DEFAULT NULL,
  `device_model` VARCHAR(100) DEFAULT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,
  `is_on_break` TINYINT(1) NOT NULL DEFAULT 0,
  `total_break_minutes` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_emp_date` (`employee_id`, `date`),
  INDEX `idx_date_status` (`date`, `status`),
  INDEX `idx_emp_status` (`employee_id`, `status`),
  CONSTRAINT `fk_attendance_employee` FOREIGN KEY (`employee_id`) 
    REFERENCES `employees` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. Table: attendance_corrections
-- Manual regularizations submitted by employees for manager review
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attendance_corrections` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `attendance_id` BIGINT UNSIGNED NOT NULL,
  `employee_id` BIGINT UNSIGNED NOT NULL,
  `date` DATE NOT NULL,
  `original_check_in` TIME DEFAULT NULL,
  `original_check_out` TIME DEFAULT NULL,
  `requested_check_in` TIME NOT NULL,
  `requested_check_out` TIME NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `reviewed_by` VARCHAR(100) DEFAULT NULL,
  `submitted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_correction_status` (`status`),
  CONSTRAINT `fk_correction_attendance` FOREIGN KEY (`attendance_id`) 
    REFERENCES `attendance` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_correction_employee` FOREIGN KEY (`employee_id`) 
    REFERENCES `employees` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. Table: leave_types
-- Master category definitions (Casual, Sick, Earned, Maternity, Comp-Off)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_types` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type_code` VARCHAR(20) NOT NULL UNIQUE,
  `type_name` VARCHAR(80) NOT NULL,
  `default_annual_quota` INT UNSIGNED NOT NULL DEFAULT 12,
  `is_paid` TINYINT(1) NOT NULL DEFAULT 1,
  `requires_attachment` TINYINT(1) NOT NULL DEFAULT 0,
  `description` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. Table: leave_balances
-- Casual (CL), Sick (SL), and Earned (EL) allowances and consumed days
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_balances` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT UNSIGNED NOT NULL UNIQUE,
  `fiscal_year` INT UNSIGNED NOT NULL DEFAULT 2026,
  `casual_total` INT UNSIGNED NOT NULL DEFAULT 12,
  `casual_used` INT UNSIGNED NOT NULL DEFAULT 0,
  `sick_total` INT UNSIGNED NOT NULL DEFAULT 10,
  `sick_used` INT UNSIGNED NOT NULL DEFAULT 0,
  `earned_total` INT UNSIGNED NOT NULL DEFAULT 18,
  `earned_used` INT UNSIGNED NOT NULL DEFAULT 0,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_balance_employee` FOREIGN KEY (`employee_id`) 
    REFERENCES `employees` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. Table: leave_applications
-- Online leave submission, date ranges, reasons, and approvals
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_applications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT UNSIGNED NOT NULL,
  `leave_type` ENUM('CASUAL', 'SICK', 'EARNED', 'MATERNITY', 'COMP_OFF') NOT NULL DEFAULT 'CASUAL',
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `number_of_days` INT UNSIGNED NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `applied_date` DATE NOT NULL,
  `reviewed_by` VARCHAR(100) DEFAULT NULL,
  `manager_remarks` TEXT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_leave_status` (`status`),
  INDEX `idx_leave_dates` (`start_date`, `end_date`),
  CONSTRAINT `fk_leave_employee` FOREIGN KEY (`employee_id`) 
    REFERENCES `employees` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. Table: holiday_schedules
-- Public, national, religious festivals, and company holiday calendars
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `holiday_schedules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(120) NOT NULL,
  `date` DATE NOT NULL UNIQUE,
  `day_of_week` VARCHAR(20) NOT NULL,
  `type` ENUM('PUBLIC', 'NATIONAL', 'FESTIVAL', 'OPTIONAL', 'COMPANY') NOT NULL DEFAULT 'PUBLIC',
  `is_mandatory` TINYINT(1) NOT NULL DEFAULT 1,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_holiday_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 12. Analytical SQL Views
-- ----------------------------------------------------------------------------

-- View A: Daily Attendance Summary for Executive Dashboards
CREATE OR REPLACE VIEW `vw_daily_attendance_summary` AS
SELECT 
  a.`date`,
  COUNT(a.`id`) AS `total_punched`,
  SUM(CASE WHEN a.`status` = 'PRESENT' THEN 1 ELSE 0 END) AS `present_count`,
  SUM(CASE WHEN a.`status` = 'LATE' THEN 1 ELSE 0 END) AS `late_count`,
  SUM(CASE WHEN a.`status` = 'HALF_DAY' THEN 1 ELSE 0 END) AS `half_day_count`,
  SUM(CASE WHEN a.`status` = 'ABSENT' THEN 1 ELSE 0 END) AS `absent_count`,
  ROUND(AVG(a.`working_minutes`) / 60, 2) AS `avg_working_hours`,
  SUM(a.`overtime_minutes`) AS `total_overtime_minutes`
FROM `attendance` a
GROUP BY a.`date`;

-- View B: Employee Available Leave Balances Breakdown
CREATE OR REPLACE VIEW `vw_employee_leave_balances` AS
SELECT 
  e.`id` AS `employee_id`,
  e.`emp_code`,
  e.`full_name`,
  e.`department`,
  b.`casual_total`,
  b.`casual_used`,
  (b.`casual_total` - b.`casual_used`) AS `casual_remaining`,
  b.`sick_total`,
  b.`sick_used`,
  (b.`sick_total` - b.`sick_used`) AS `sick_remaining`,
  b.`earned_total`,
  b.`earned_used`,
  (b.`earned_total` - b.`earned_used`) AS `earned_remaining`
FROM `employees` e
LEFT JOIN `leave_balances` b ON b.`employee_id` = e.`id`;

-- View C: Monthly Working Hours & Overtime Audit per Employee
CREATE OR REPLACE VIEW `vw_monthly_working_hours` AS
SELECT 
  e.`id` AS `employee_id`,
  e.`emp_code`,
  e.`full_name`,
  DATE_FORMAT(a.`date`, '%Y-%m') AS `month_year`,
  COUNT(a.`id`) AS `days_present`,
  ROUND(SUM(a.`working_minutes`) / 60, 2) AS `total_working_hours`,
  ROUND(SUM(a.`overtime_minutes`) / 60, 2) AS `total_overtime_hours`,
  SUM(a.`late_minutes`) AS `total_late_minutes`
FROM `employees` e
JOIN `attendance` a ON a.`employee_id` = e.`id`
GROUP BY e.`id`, DATE_FORMAT(a.`date`, '%Y-%m');

-- ----------------------------------------------------------------------------
-- 13. Stored Procedures
-- ----------------------------------------------------------------------------

DELIMITER //

-- Procedure 1: sp_record_attendance_punch
-- Handles Geofenced Check-in, Mock GPS validation, and Shift Late Calculation
CREATE PROCEDURE `sp_record_attendance_punch`(
  IN p_emp_id BIGINT,
  IN p_lat DECIMAL(10, 8),
  IN p_lng DECIMAL(11, 8),
  IN p_device_id VARCHAR(100),
  IN p_device_model VARCHAR(100),
  IN p_ip VARCHAR(45),
  IN p_is_mock TINYINT(1),
  IN p_face_score FLOAT,
  OUT p_status_out VARCHAR(30),
  OUT p_msg_out VARCHAR(255)
)
BEGIN
  DECLARE v_shift_start TIME;
  DECLARE v_grace INT;
  DECLARE v_today DATE;
  DECLARE v_now TIME;
  DECLARE v_late_mins INT DEFAULT 0;
  DECLARE v_status VARCHAR(30) DEFAULT 'PRESENT';
  DECLARE v_dist FLOAT;

  SET v_today = CURDATE();
  SET v_now = CURTIME();

  -- Security: Reject Fake GPS
  IF p_is_mock = 1 THEN
    SET p_status_out = 'REJECTED';
    SET p_msg_out = 'Security Alert: Mock / Fake GPS location detected. Punch rejected.';
  ELSE
    -- Calculate Distance to Office HQ (28.613939, 77.209021) in meters
    SET v_dist = (6371000 * ACOS(COS(RADIANS(28.613939)) * COS(RADIANS(p_lat)) * COS(RADIANS(p_lng) - RADIANS(77.209021)) + SIN(RADIANS(28.613939)) * SIN(RADIANS(p_lat))));

    -- Fetch Shift Timings
    SELECT s.`start_time`, s.`grace_period_minutes` INTO v_shift_start, v_grace
    FROM `shifts` s
    JOIN `employees` e ON e.`shift_id` = s.`id`
    WHERE e.`id` = p_emp_id;

    -- Evaluate Late Punch
    IF TIMESTAMPDIFF(MINUTE, v_shift_start, v_now) > v_grace THEN
      SET v_late_mins = TIMESTAMPDIFF(MINUTE, v_shift_start, v_now);
      SET v_status = 'LATE';
    END IF;

    -- Insert or Update Attendance Record
    INSERT INTO `attendance` (
      `employee_id`, `date`, `check_in_time`, `check_in_timestamp`,
      `late_minutes`, `status`, `latitude`, `longitude`, `is_geofenced`,
      `distance_meters`, `is_face_verified`, `face_match_confidence`,
      `is_mock_gps_detected`, `device_id`, `device_model`, `ip_address`
    ) VALUES (
      p_emp_id, v_today, v_now, UNIX_TIMESTAMP() * 1000,
      v_late_mins, v_status, p_lat, p_lng, (v_dist <= 250),
      v_dist, 1, p_face_score, 0, p_device_id, p_device_model, p_ip
    )
    ON DUPLICATE KEY UPDATE
      `check_in_time` = VALUES(`check_in_time`),
      `status` = VALUES(`status`);

    SET p_status_out = v_status;
    SET p_msg_out = CONCAT('Attendance recorded at ', v_now, ' (Status: ', v_status, ')');
  END IF;
END //

-- Procedure 2: sp_approve_leave_request
-- Approves leave and automatically deducts from employee leave_balances
CREATE PROCEDURE `sp_approve_leave_request`(
  IN p_leave_id BIGINT,
  IN p_reviewed_by VARCHAR(100),
  IN p_remarks TEXT
)
BEGIN
  DECLARE v_emp_id BIGINT;
  DECLARE v_type VARCHAR(20);
  DECLARE v_days INT;

  SELECT `employee_id`, `leave_type`, `number_of_days` 
  INTO v_emp_id, v_type, v_days
  FROM `leave_applications`
  WHERE `id` = p_leave_id;

  -- Mark Application Approved
  UPDATE `leave_applications`
  SET `status` = 'APPROVED', `reviewed_by` = p_reviewed_by, `manager_remarks` = p_remarks
  WHERE `id` = p_leave_id;

  -- Deduct Balances
  IF v_type = 'CASUAL' THEN
    UPDATE `leave_balances` SET `casual_used` = `casual_used` + v_days WHERE `employee_id` = v_emp_id;
  ELSEIF v_type = 'SICK' THEN
    UPDATE `leave_balances` SET `sick_used` = `sick_used` + v_days WHERE `employee_id` = v_emp_id;
  ELSEIF v_type = 'EARNED' THEN
    UPDATE `leave_balances` SET `earned_used` = `earned_used` + v_days WHERE `employee_id` = v_emp_id;
  END IF;
END //

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 14. Initial Seed Data
-- ----------------------------------------------------------------------------

-- Departments
INSERT INTO `departments` (`id`, `dept_code`, `dept_name`, `description`) VALUES
(1, 'ENG', 'Engineering', 'Software & Infrastructure Development'),
(2, 'HR', 'Human Resources', 'Talent Acquisition & Employee Operations'),
(3, 'PROD', 'Product & Design', 'UI/UX and Product Architecture'),
(4, 'OPS', 'Operations & Support', 'Global Cloud Operations & IT Infrastructure'),
(5, 'QA', 'Quality Assurance', 'Test Automation & Compliance')
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);

-- Designations
INSERT INTO `designations` (`id`, `department_id`, `title`, `grade_level`) VALUES
(1, 1, 'Senior Android Lead', 'L4'),
(2, 2, 'HR Operations Lead', 'L3'),
(3, 3, 'UI/UX Product Architect', 'L4'),
(4, 4, 'Infrastructure Engineer', 'L2'),
(5, 5, 'QA Automation Specialist', 'L2')
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- Shifts
INSERT INTO `shifts` (`id`, `shift_name`, `start_time`, `end_time`, `grace_period_minutes`, `break_duration_minutes`, `is_flexible`, `weekly_off_days`, `description`) VALUES
(1, 'General Morning Shift', '09:00:00', '18:00:00', 15, 45, 0, 'Saturday,Sunday', 'Core business hours with 15m late grace period'),
(2, 'Evening Operations Shift', '14:00:00', '23:00:00', 15, 45, 0, 'Saturday,Sunday', 'Evening regional support and maintenance'),
(3, 'Night Support Shift', '22:00:00', '07:00:00', 20, 60, 0, 'Sunday,Monday', '24/7 client uptime and infrastructure shift'),
(4, 'Flexible Hours (8h Target)', '08:00:00', '20:00:00', 30, 60, 1, 'Saturday,Sunday', 'Self-scheduled 8 hours daily target')
ON DUPLICATE KEY UPDATE `shift_name` = VALUES(`shift_name`);

-- Leave Types
INSERT INTO `leave_types` (`id`, `type_code`, `type_name`, `default_annual_quota`, `is_paid`, `requires_attachment`) VALUES
(1, 'CASUAL', 'Casual Leave', 12, 1, 0),
(2, 'SICK', 'Sick Leave', 10, 1, 1),
(3, 'EARNED', 'Earned / Privilege Leave', 18, 1, 0),
(4, 'COMP_OFF', 'Compensatory Off', 6, 1, 0)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

-- Employees
INSERT INTO `employees` (`id`, `emp_code`, `full_name`, `email`, `mobile`, `address`, `department_id`, `designation_id`, `department`, `designation`, `joining_date`, `is_active`, `shift_id`, `registered_device_id`, `registered_device_model`, `emergency_contact`) VALUES
(1, 'EMP-1001', 'Aarav Sharma', 'aarav.sharma@enterprise.com', '+91 98765 43210', 'Flat 402, Lotus Greens, Sector 62, Noida', 1, 1, 'Engineering', 'Senior Android Lead', '2022-03-15', 1, 1, 'DEV-ANDROID-001', 'Google Pixel 8', '+91 98111 22334'),
(2, 'EMP-1002', 'Priya Sen', 'priya.sen@enterprise.com', '+91 98234 56789', 'B-12 Cyber Heights, Whitefield, Bangalore', 2, 2, 'Human Resources', 'HR Operations Lead', '2021-08-01', 1, 1, 'DEV-SAMSUNG-002', 'Samsung Galaxy S24', '+91 98444 55667'),
(3, 'EMP-1003', 'Rohan Mehta', 'rohan.mehta@enterprise.com', '+91 97112 34567', 'Tower 4, DLF Phase 5, Gurugram', 3, 3, 'Product & Design', 'UI/UX Product Architect', '2023-01-10', 1, 4, 'DEV-ONEPLUS-003', 'OnePlus 12', '+91 97222 33445'),
(4, 'EMP-1004', 'Ananya Roy', 'ananya.roy@enterprise.com', '+91 99345 67890', '301 Sunrise Palms, Andheri East, Mumbai', 4, 4, 'Operations & Support', 'Infrastructure Engineer', '2023-06-20', 1, 2, 'DEV-PIXEL-004', 'Google Pixel 7a', '+91 99555 66778'),
(5, 'EMP-1005', 'Vikram Malhotra', 'vikram.m@enterprise.com', '+91 96500 11223', 'Villa 18, Palm Meadows, Hyderabad', 5, 5, 'Quality Assurance', 'QA Automation Specialist', '2020-11-15', 0, 1, 'DEV-XIAOMI-005', 'Xiaomi 14', '+91 96555 77889')
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- Employee Documents
INSERT INTO `employee_documents` (`id`, `employee_id`, `document_type`, `document_name`, `file_url`) VALUES
(1, 1, 'Identity Proof', 'Aadhaar_National_ID.pdf', '/uploads/docs/emp1_aadhaar.pdf'),
(2, 1, 'Educational Degree', 'BTech_Degree_Certificate.pdf', '/uploads/docs/emp1_degree.pdf'),
(3, 1, 'Corporate Contract', 'Signed_Offer_Letter.pdf', '/uploads/docs/emp1_offer.pdf'),
(4, 2, 'Passport Copy', 'Passport_PriyaSen.pdf', '/uploads/docs/emp2_passport.pdf'),
(5, 3, 'Driving License', 'DL_RohanMehta.pdf', '/uploads/docs/emp3_dl.pdf')
ON DUPLICATE KEY UPDATE `document_name` = VALUES(`document_name`);

-- Leave Balances
INSERT INTO `leave_balances` (`employee_id`, `casual_total`, `casual_used`, `sick_total`, `sick_used`, `earned_total`, `earned_used`) VALUES
(1, 12, 3, 10, 1, 18, 5),
(2, 12, 2, 10, 0, 18, 3),
(3, 12, 1, 10, 2, 18, 4),
(4, 12, 4, 10, 1, 18, 2),
(5, 12, 0, 10, 0, 18, 0)
ON DUPLICATE KEY UPDATE `casual_total` = VALUES(`casual_total`);

-- Attendance Records
INSERT INTO `attendance` (`id`, `employee_id`, `date`, `check_in_time`, `check_out_time`, `check_in_timestamp`, `check_out_timestamp`, `working_minutes`, `late_minutes`, `early_exit_minutes`, `overtime_minutes`, `status`, `latitude`, `longitude`, `location_address`, `is_geofenced`, `distance_meters`, `is_face_verified`, `face_match_confidence`, `is_mock_gps_detected`, `device_id`, `device_model`, `ip_address`) VALUES
(1, 1, '2026-11-07', '09:05:22', '18:18:40', 1794029122000, 1794062320000, 553, 0, 0, 18, 'PRESENT', 28.613939, 77.209021, 'Enterprise HQ Tower B (Geofenced)', 1, 35.0, 1, 98.7, 0, 'DEV-ANDROID-001', 'Google Pixel 8', '192.168.1.108'),
(2, 1, '2026-11-06', '09:32:10', '18:05:00', 1793943130000, 1793973900000, 512, 17, 0, 0, 'LATE', 28.614100, 77.209200, 'Enterprise HQ Main Lobby', 1, 48.0, 1, 96.2, 0, 'DEV-ANDROID-001', 'Google Pixel 8', '192.168.1.108'),
(3, 2, '2026-11-07', '08:58:15', '18:02:10', 1794028695000, 1794061330000, 543, 0, 0, 2, 'PRESENT', 28.613800, 77.208900, 'Enterprise HQ, HR Wing', 1, 22.0, 1, 99.1, 0, 'DEV-SAMSUNG-002', 'Samsung Galaxy S24', '192.168.1.114')
ON DUPLICATE KEY UPDATE `working_minutes` = VALUES(`working_minutes`);

-- Leave Applications
INSERT INTO `leave_applications` (`id`, `employee_id`, `leave_type`, `start_date`, `end_date`, `number_of_days`, `reason`, `status`, `applied_date`, `reviewed_by`, `manager_remarks`) VALUES
(1, 1, 'CASUAL', '2026-11-14', '2026-11-15', 2, 'Family function & personal travel', 'PENDING', '2026-11-06', NULL, NULL),
(2, 2, 'SICK', '2026-11-02', '2026-11-02', 1, 'Severe migraine & doctor consultation', 'APPROVED', '2026-11-01', 'HR Director', 'Approved. Get well soon.'),
(3, 4, 'EARNED', '2026-11-20', '2026-11-24', 5, 'Annual vacation with family', 'PENDING', '2026-11-05', NULL, NULL)
ON DUPLICATE KEY UPDATE `reason` = VALUES(`reason`);

-- Holiday Schedules
INSERT INTO `holiday_schedules` (`id`, `name`, `date`, `day_of_week`, `type`, `description`) VALUES
(1, 'New Year\'s Day', '2026-01-01', 'Thursday', 'NATIONAL', 'Global celebration marking the new year'),
(2, 'Republic Day', '2026-01-26', 'Monday', 'NATIONAL', 'Constitution of India commemoration'),
(3, 'Holi Festival', '2026-03-04', 'Wednesday', 'FESTIVAL', 'Festival of colors and spring'),
(4, 'Good Friday', '2026-04-03', 'Friday', 'PUBLIC', 'Christian holiday commemorating the passion'),
(5, 'Eid-ul-Fitr', '2026-03-20', 'Friday', 'FESTIVAL', 'Celebration marking the end of Ramadan'),
(6, 'Independence Day', '2026-08-15', 'Saturday', 'NATIONAL', 'National Independence Day celebration'),
(7, 'Gandhi Jayanti', '2026-10-02', 'Friday', 'NATIONAL', 'Birth anniversary of Mahatma Gandhi'),
(8, 'Diwali (Festival of Lights)', '2026-11-09', 'Monday', 'FESTIVAL', 'Major festival celebrating prosperity & light'),
(9, 'Christmas Day', '2026-12-25', 'Friday', 'PUBLIC', 'Christmas celebration and company holiday')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
""".trimIndent()
    }

    fun generatePhpConnectionCode(): String {
        return """
<?php
// ============================================================================
// db_connect.php - High Performance PDO Database Connection
// ============================================================================

${'$'}host = '127.0.0.1';
${'$'}db   = 'workforce_hr_db';
${'$'}user = 'root';
${'$'}pass = 'your_strong_password';
${'$'}port = 3306;
${'$'}charset = 'utf8mb4';

${'$'}dsn = "mysql:host=${'$'}host;port=${'$'}port;dbname=${'$'}db;charset=${'$'}charset";
${'$'}options = [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
];

try {
    ${'$'}pdo = new PDO(${'$'}dsn, ${'$'}user, ${'$'}pass, ${'$'}options);
} catch (\PDOException ${'$'}e) {
    header('Content-Type: application/json', true, 500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . ${'$'}e->getMessage()
    ]);
    exit;
}
?>
""".trimIndent()
    }

    fun generatePhpAttendanceApiCode(): String {
        return """
<?php
// ============================================================================
// api_attendance.php - Check-in / Check-out with Geofence & Face Validation
// ============================================================================
header('Content-Type: application/json');
require_once 'db_connect.php';

${'$'}action = ${'$'}_GET['action'] ?? '';
${'$'}input = json_decode(file_get_contents('php://input'), true);

if (${'$'}action === 'check_in') {
    ${'$'}empId = ${'$'}input['employee_id'];
    ${'$'}lat = ${'$'}input['latitude'];
    ${'$'}lng = ${'$'}input['longitude'];
    ${'$'}deviceId = ${'$'}input['device_id'];
    ${'$'}isMock = ${'$'}input['is_mock_gps'] ?? false;
    ${'$'}faceConfidence = ${'$'}input['face_confidence'] ?? 0;
    ${'$'}date = date('Y-m-d');
    ${'$'}time = date('H:i:s');
    ${'$'}timestamp = round(microtime(true) * 1000);
    ${'$'}clientIp = ${'$'}_SERVER['REMOTE_ADDR'];

    // Security Verification: Fake GPS Rejection
    if (${'$'}isMock) {
        echo json_encode(['success' => false, 'message' => 'Fraud Detection: Fake GPS detected! Punch rejected.']);
        exit;
    }

    // Geofencing Calculation (Office Coords: 28.6139, 77.2090)
    ${'$'}officeLat = 28.613939;
    ${'$'}officeLng = 77.209021;
    ${'$'}theta = ${'$'}lng - ${'$'}officeLng;
    ${'$'}dist = sin(deg2rad(${'$'}lat)) * sin(deg2rad(${'$'}officeLat)) +  cos(deg2rad(${'$'}lat)) * cos(deg2rad(${'$'}officeLat)) * cos(deg2rad(${'$'}theta));
    ${'$'}dist = acos(${'$'}dist);
    ${'$'}dist = rad2deg(${'$'}dist);
    ${'$'}meters = ${'$'}dist * 60 * 1.1515 * 1.609344 * 1000;
    ${'$'}isGeofenced = (${'$'}meters <= 250) ? 1 : 0;

    // Shift Late Marking Check
    ${'$'}stmtShift = ${'$'}pdo->prepare("SELECT s.* FROM shifts s JOIN employees e ON e.shift_id = s.id WHERE e.id = ?");
    ${'$'}stmtShift->execute([${'$'}empId]);
    ${'$'}shift = ${'$'}stmtShift->fetch();

    ${'$'}shiftStart = strtotime(${'$'}date . ' ' . (${'$'}shift ? ${'$'}shift['start_time'] : '09:00:00'));
    ${'$'}grace = (${'$'}shift ? ${'$'}shift['grace_period_minutes'] : 15) * 60;
    ${'$'}actualPunch = strtotime(${'$'}date . ' ' . ${'$'}time);

    ${'$'}lateMinutes = 0;
    ${'$'}status = 'PRESENT';
    if (${'$'}actualPunch > (${'$'}shiftStart + ${'$'}grace)) {
        ${'$'}lateMinutes = round((${'$'}actualPunch - ${'$'}shiftStart) / 60);
        ${'$'}status = 'LATE';
    }

    ${'$'}stmt = ${'$'}pdo->prepare("
        INSERT INTO attendance (
            employee_id, date, check_in_time, check_in_timestamp,
            late_minutes, status, latitude, longitude, is_geofenced,
            distance_meters, is_face_verified, face_match_confidence,
            is_mock_gps_detected, device_id, ip_address
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 0, ?, ?)
        ON DUPLICATE KEY UPDATE 
            check_in_time = VALUES(check_in_time),
            status = VALUES(status)
    ");
    ${'$'}stmt->execute([
        ${'$'}empId, ${'$'}date, ${'$'}time, ${'$'}timestamp,
        ${'$'}lateMinutes, ${'$'}status, ${'$'}lat, ${'$'}lng, ${'$'}isGeofenced,
        ${'$'}meters, ${'$'}faceConfidence, ${'$'}deviceId, ${'$'}clientIp
    ]);

    echo json_encode([
        'success' => true,
        'message' => 'Check-in recorded successfully!',
        'status' => ${'$'}status,
        'time' => ${'$'}time,
        'is_geofenced' => (bool)${'$'}isGeofenced,
        'late_minutes' => ${'$'}lateMinutes
    ]);
    exit;
}

if (${'$'}action === 'check_out') {
    ${'$'}empId = ${'$'}input['employee_id'];
    ${'$'}date = date('Y-m-d');
    ${'$'}time = date('H:i:s');
    ${'$'}timestamp = round(microtime(true) * 1000);

    ${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM attendance WHERE employee_id = ? AND date = ?");
    ${'$'}stmt->execute([${'$'}empId, ${'$'}date]);
    ${'$'}row = ${'$'}stmt->fetch();

    if (!${'$'}row) {
        echo json_encode(['success' => false, 'message' => 'No active check-in found for today']);
        exit;
    }

    ${'$'}inTime = strtotime(${'$'}date . ' ' . ${'$'}row['check_in_time']);
    ${'$'}outTime = strtotime(${'$'}date . ' ' . ${'$'}time);
    ${'$'}workingMinutes = round((${'$'}outTime - ${'$'}inTime) / 60);

    // Half day / Full day calculation
    ${'$'}status = ${'$'}row['status'];
    if (${'$'}workingMinutes < 270) { // Under 4.5 hours
        ${'$'}status = 'HALF_DAY';
    }

    ${'$'}overtime = (${'$'}workingMinutes > 540) ? (${'$'}workingMinutes - 540) : 0;

    ${'$'}update = ${'$'}pdo->prepare("
        UPDATE attendance SET
            check_out_time = ?,
            check_out_timestamp = ?,
            working_minutes = ?,
            overtime_minutes = ?,
            status = ?
        WHERE id = ?
    ");
    ${'$'}update->execute([${'$'}time, ${'$'}timestamp, ${'$'}workingMinutes, ${'$'}overtime, ${'$'}status, ${'$'}row['id']]);

    echo json_encode([
        'success' => true,
        'message' => 'Check-out successfully completed',
        'working_hours' => round(${'$'}workingMinutes / 60, 2),
        'status' => ${'$'}status,
        'overtime_minutes' => ${'$'}overtime
    ]);
    exit;
}
?>
""".trimIndent()
    }
}
