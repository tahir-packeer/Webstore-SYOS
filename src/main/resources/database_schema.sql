CREATE DATABASE IF NOT EXISTS syos;
USE syos;

-- Users table 
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    type ENUM('cashier', 'store manager', 'admin') NOT NULL
);

-- Items table
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

-- Stock table - stores inventory with purchase and expiry dates
CREATE TABLE IF NOT EXISTS stock (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    date_of_purchase DATE NOT NULL,
    date_of_expiry DATE NOT NULL,
    availability BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Shelf table - manages both store and website inventory
CREATE TABLE IF NOT EXISTS shelf (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    type ENUM('STORE', 'WEBSITE') NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    UNIQUE KEY unique_item_type (item_id, type)
);

-- Regular customers table (for in-store POS system)
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contactNumber VARCHAR(20) NOT NULL UNIQUE,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Online customers table (for website)
CREATE TABLE IF NOT EXISTS online_customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contactNumber VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    password VARCHAR(255) NOT NULL,
    registrationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bills table - enhanced to support transaction and store types
CREATE TABLE IF NOT EXISTS bill (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    customer_type ENUM('REGULAR', 'ONLINE') DEFAULT 'REGULAR',
    invoiceNumber VARCHAR(20) UNIQUE NOT NULL,
    fullPrice DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) DEFAULT 0,
    cashTendered DECIMAL(10, 2) DEFAULT 0,
    changeAmount DECIMAL(10, 2) DEFAULT 0,
    billDate DATE NOT NULL,
    transactionType ENUM('COUNTER', 'ONLINE') DEFAULT 'COUNTER',
    storeType ENUM('STORE', 'WEBSITE') DEFAULT 'STORE'
);

-- Bill items table
CREATE TABLE IF NOT EXISTS billItem (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    bill_id INT NOT NULL,
    quantity INT NOT NULL,
    itemPrice DECIMAL(10, 2) NOT NULL,
    totalPrice DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bill_id) REFERENCES bill(id)
);

-- Shelf stock tracking table (for tracking which stock batches go to which shelves)
CREATE TABLE IF NOT EXISTS shelf_stock (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stock_id INT NOT NULL,
    shelf_id INT NOT NULL,
    quantity_moved INT NOT NULL,
    move_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stock_id) REFERENCES stock(id),
    FOREIGN KEY (shelf_id) REFERENCES shelf(id)
);

INSERT IGNORE INTO users (name, password, type) VALUES 
('admin', 'admin123', 'admin'),
('cashier1', 'cash123', 'cashier'),
('manager1', 'store123', 'store manager');

INSERT IGNORE INTO items (code, name, price) VALUES 
('RICE001', 'Basmati Rice 1kg', 450),
('MILK001', 'Fresh Milk 1L', 275),
('BREAD001', 'White Bread', 125),
('EGGS001', 'Free Range Eggs 12pc', 320),
('SUGAR001', 'White Sugar 1kg', 210),
('TEA001', 'Ceylon Black Tea 100g', 580),
('OIL001', 'Coconut Oil 500ml', 625),
('FLOUR001', 'Wheat Flour 1kg', 315),
('SALT001', 'Sea Salt 1kg', 150),
('ONION001', 'Red Onions 1kg', 295);

-- Store shelf inventory (physical store)
INSERT IGNORE INTO shelf (item_id, quantity, type) VALUES 
(1, 75, 'STORE'),
(2, 45, 'STORE'),
(3, 120, 'STORE'),
(4, 60, 'STORE'),
(5, 85, 'STORE'),
(6, 30, 'STORE'),
(7, 25, 'STORE'),
(8, 90, 'STORE'),
(9, 150, 'STORE'),
(10, 40, 'STORE');

-- Website shelf inventory (online catalog)
INSERT IGNORE INTO shelf (item_id, quantity, type) VALUES 
(1, 50, 'WEBSITE'),
(2, 30, 'WEBSITE'),
(3, 80, 'WEBSITE'),
(4, 35, 'WEBSITE'),
(5, 60, 'WEBSITE'),
(6, 20, 'WEBSITE'),
(7, 15, 'WEBSITE'),
(8, 65, 'WEBSITE'),
(9, 100, 'WEBSITE'),
(10, 25, 'WEBSITE');

INSERT IGNORE INTO stock (item_id, quantity, date_of_purchase, date_of_expiry, availability) VALUES 
(1, 200, '2024-09-01', '2025-03-01', TRUE),
(1, 150, '2024-09-15', '2025-03-15', TRUE),
(2, 100, '2024-09-25', '2024-10-05', TRUE),
(2, 80, '2024-09-28', '2024-10-08', TRUE),
(3, 250, '2024-09-20', '2024-10-02', TRUE),
(4, 120, '2024-09-22', '2024-10-22', TRUE),
(5, 180, '2024-09-10', '2025-09-10', TRUE),
(6, 75, '2024-09-05', '2025-09-05', TRUE),
(7, 60, '2024-09-12', '2025-09-12', TRUE),
(8, 200, '2024-09-08', '2025-06-08', TRUE),
(9, 300, '2024-09-01', '2026-09-01', TRUE),
(10, 100, '2024-09-26', '2024-10-10', TRUE);

-- Discount codes table
CREATE TABLE IF NOT EXISTS discount_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);