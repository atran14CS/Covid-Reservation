CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers(Username),
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY(Name)
);

CREATE TABLE Patient (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointment (
    AppID int,
    PUsername varchar(255),
    CUsername varchar(255),
    Name varchar(255),
    Time date,
    PRIMARY KEY(AppID),
    FOREIGN KEY (PUsername) REFERENCES Patient(Username),
    FOREIGN KEY (CUsername) REFERENCES Caregivers(Username),
    FOREIGN KEY (Name) REFERENCES Vaccines(Name),
);
