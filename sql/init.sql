IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'stock_control')
BEGIN
    CREATE DATABASE stock_control;
END
GO

USE stock_control;
GO

IF NOT EXISTS (SELECT * FROM sys.server_principals WHERE name = 'stock_user')
BEGIN
    CREATE LOGIN stock_user WITH PASSWORD = 'StockUser123!';
END
GO

IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'stock_user')
BEGIN
    CREATE USER stock_user FOR LOGIN stock_user;
END
GO

ALTER ROLE db_owner ADD MEMBER stock_user;
GO
