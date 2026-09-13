ALTER TABLE bikes
    RENAME COLUMN suspension_type TO suspension_layout;

ALTER TABLE bikes
    RENAME CONSTRAINT bikes_suspension_type_not_blank_check
        TO bikes_suspension_layout_not_blank_check;
