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
('admin', 'admin123', 'manager'),
('cashier1', 'cash123', 'cashier'),
('manager1', 'store123', 'storemanager');

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

-- Discount codes table
CREATE TABLE IF NOT EXISTS discount_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);