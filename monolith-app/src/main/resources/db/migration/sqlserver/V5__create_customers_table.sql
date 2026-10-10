-- NexPOS · SQL Server · V5: clientes (equivalente a mysql/V5)

CREATE TABLE customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    doc_type NVARCHAR(10) NOT NULL DEFAULT 'CC', -- CC, NIT, CE, PASAPORTE, TI
    doc_number NVARCHAR(50) NOT NULL CONSTRAINT uq_customers_doc_number UNIQUE,
    name NVARCHAR(150) NOT NULL,
    email NVARCHAR(150) NULL,
    phone NVARCHAR(50) NULL,
    address NVARCHAR(255) NULL,
    city NVARCHAR(100) DEFAULT 'Cali',
    department NVARCHAR(100) DEFAULT 'Valle del Cauca',
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    INDEX idx_customers_doc_number (doc_number),
    INDEX idx_customers_name (name)
);

SET IDENTITY_INSERT customers ON;

INSERT INTO customers (id, doc_type, doc_number, name, email, phone, address, city, department)
VALUES (1, N'CC', N'222222222222', N'Consumidor Final', N'facturacion@nexpos.com.co', N'3000000000', N'Venta en Mostrador POS', N'Cali', N'Valle del Cauca');

SET IDENTITY_INSERT customers OFF;

ALTER TABLE sales ADD customer_email NVARCHAR(150) NULL;
