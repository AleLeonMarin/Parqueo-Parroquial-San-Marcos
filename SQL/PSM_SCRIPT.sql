-- Sequence creation for table IDs
CREATE SEQUENCE psm_parameters_seq01 INCREMENT BY 1 START WITH 1 NOMAXVALUE NOMINVALUE NOCACHE;
CREATE SEQUENCE psm_vehicles_seq01 INCREMENT BY 1 START WITH 1 NOMAXVALUE NOMINVALUE NOCACHE;
-- Table: psm_parameters
CREATE TABLE psm_parameters (
    par_id NUMBER NOT NULL,
    par_quantity VARCHAR2(5) NOT NULL,
    par_tax VARCHAR2(10) NOT NULL,
    version NUMBER NOT NULL
);
ALTER TABLE psm_parameters
ADD CONSTRAINT psm_parameters_pk PRIMARY KEY (par_id);
-- Table: psm_vehicles
CREATE TABLE psm_vehicles (
    veh_id NUMBER NOT NULL,
    veh_owner VARCHAR2(100),
    veh_reference VARCHAR2(40) NOT NULL,
    veh_plate VARCHAR2(10) NOT NULL,
    veh_ingress_date DATE NOT NULL,
    veh_egress_date DATE,
    veh_status VARCHAR2(1) DEFAULT 'P' NOT NULL CONSTRAINT psm_vehicles_chk01 CHECK (veh_status IN ('P', 'S')),
    veh_tax VARCHAR2(30) NOT NULL,
    veh_version NUMBER NOT NULL
);
ALTER TABLE psm_vehicles
ADD CONSTRAINT psm_vehicles_pk PRIMARY KEY (veh_id);
ALTER TABLE psm_vehicles
ADD CONSTRAINT psm_vehicles_uk UNIQUE (veh_plate);
ALTER TABLE psm_vehicles
ADD CONSTRAINT psm_vehicles_reference_uk UNIQUE (veh_reference);
-- Triggers for psm_parameters
CREATE OR REPLACE TRIGGER psm_parameters_trg01 BEFORE
INSERT ON psm_parameters FOR EACH ROW BEGIN IF :new.par_id IS NULL
    OR :new.par_id <= 0 THEN :new.par_id := psm_parameters_seq01.NEXTVAL;
END IF;
END;
;
CREATE OR REPLACE TRIGGER psm_parameters_trg02
AFTER
UPDATE OF par_id ON psm_parameters FOR EACH ROW BEGIN RAISE_APPLICATION_ERROR(
        -20011,
        'No se puede actualizar el campo par_id en la tabla psm_parameters ya que utiliza una secuencia.'
    );
END;
-- Triggers for psm_vehicles
CREATE OR REPLACE TRIGGER psm_vehicles_trg01 BEFORE
INSERT ON psm_vehicles FOR EACH ROW BEGIN IF :new.veh_id IS NULL
    OR :new.veh_id <= 0 THEN :new.veh_id := psm_vehicles_seq01.NEXTVAL;
END IF;
END;
;
CREATE OR REPLACE TRIGGER psm_vehicles_trg02
AFTER
UPDATE OF veh_id ON psm_vehicles FOR EACH ROW BEGIN RAISE_APPLICATION_ERROR(
        -20012,
        'No se puede actualizar el campo veh_id en la tabla psm_vehicles ya que utiliza una secuencia.'
    );
END;
