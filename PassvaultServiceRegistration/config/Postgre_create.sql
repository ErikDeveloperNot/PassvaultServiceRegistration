------------------------
-- Create User table
------------------------
CREATE TABLE Users
(
  account_uuid    	   varchar(320)      NOT NULL ,
  account_password      varchar(1000)      NOT NULL ,
  account_last_sync     bigint             NULL ,
  account_format        numeric(4,2)   NOT NULL 
);

------------------------
-- Create Accounts table
------------------------
CREATE TABLE Accounts
(
  account_name    varchar(1000)      NOT NULL ,
  account_uuid          varchar(320)    NOT NULL ,
  user_name       varchar(1000)      NOT NULL ,
  password        varchar(1000)      NOT NULL ,
  old_password    varchar(1000)      NOT NULL ,
  url             text                   NULL ,
  update_time     bigint             NOT NULL ,
  deleted		   boolean	 	NOT NULL
);

------------------------
-- Create History table
-- populated by a trigger when accounts table is modified
------------------------
CREATE TABLE History
(
  id              bigserial          NOT NULL ,
  insert_time     bigint             NOT NULL ,
  account_name    varchar(1000)      NOT NULL ,
  account_uuid    varchar(320)          NOT NULL ,
  user_name       varchar(1000)      NOT NULL ,
  password        varchar(1000)      NOT NULL ,
  old_password    varchar(1000)      NOT NULL ,
  url             text                   NULL ,
  update_time     bigint             NOT NULL,
  deleted		   boolean	 	NOT NULL
);

----------------------
-- Define primary keys
----------------------
ALTER TABLE Users ADD PRIMARY KEY (account_uuid);
ALTER TABLE Accounts ADD PRIMARY KEY (account_name, account_uuid);
--ALTER TABLE History ADD PRIMARY KEY (account_name, account_uuid, insert_time);
ALTER TABLE History ADD PRIMARY KEY (id);


----------------------
-- Define foreign keys
----------------------
ALTER TABLE Accounts ADD CONSTRAINT FK_Accounts_Users FOREIGN KEY (account_uuid) REFERENCES Users (account_uuid) ON DELETE CASCADE;
-- no FK Constraint on the history table
--ALTER TABLE History ADD CONSTRAINT FK_History_Users FOREIGN KEY (account_uuid) REFERENCES Users (account_uuid);

----------------------
-- Define triggers
----------------------
CREATE OR REPLACE FUNCTION aft_delete_acct()
  RETURNS trigger AS
$$
BEGIN
INSERT into History (insert_time, account_name, account_uuid, user_name, password, old_password, url, update_time, deleted)
VALUES (round(extract(epoch from now())), OLD.account_name, OLD.account_uuid, OLD.user_name, OLD.password, OLD.old_password, OLD.url, OLD.update_time, OLD.deleted);
RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';

CREATE TRIGGER delete_acct
  AFTER DELETE
  ON Accounts
  FOR EACH ROW
  EXECUTE PROCEDURE aft_delete_acct();
  
CREATE TRIGGER update_acct
  AFTER UPDATE
  ON Accounts
  FOR EACH ROW
  EXECUTE PROCEDURE aft_delete_acct();
