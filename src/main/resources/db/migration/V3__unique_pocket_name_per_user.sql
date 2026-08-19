CREATE UNIQUE INDEX uk_pockets_user_id_lower_name ON pockets (user_id, LOWER(name));
