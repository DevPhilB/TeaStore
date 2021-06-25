# noinspection SqlNoDataSourceInspectionForFile

CREATE DATABASE teastore;
CREATE USER 'teastore-admin'@'%' IDENTIFIED BY 'teastore-pw';
GRANT ALL PRIVILEGES ON teastore.* TO 'teastore-admin'@'%';
FLUSH PRIVILEGES;
