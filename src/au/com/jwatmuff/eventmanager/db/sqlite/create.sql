
CREATE TABLE IF NOT EXISTS competition (
  id INTEGER NOT NULL,
  name VARCHAR(512) NOT NULL,
  location VARCHAR(512),
  start_date DATE,
  end_date DATE,
  age_threshold_date DATE,
  mats INTEGER NOT NULL,
  password_hash INTEGER,
  wi_password_hash INTEGER,
  pd_password_hash INTEGER,
  sb_password_hash INTEGER,
  license_name VARCHAR(255),
  license_type VARCHAR(255),
  license_contact VARCHAR(255),
  director_name VARCHAR(255),
  director_contact VARCHAR(255),
  draw_configuration VARCHAR(255),
  closed BOOL NOT NULL DEFAULT 'false',
  last_updated TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS player_details (
  id INTEGER NOT NULL,
  club VARCHAR(255),
  home_phone VARCHAR(20),
  work_phone VARCHAR(20),
  mobile VARCHAR(20),
  addr_street VARCHAR(255),
  addr_city VARCHAR(255),
  addr_postcode VARCHAR(4),
  addr_state VARCHAR(3),
  email VARCHAR(512),
  emergency_name VARCHAR(255),
  emergency_phone VARCHAR(20),
  emergency_mobile VARCHAR(20),
  medical_conditions TEXT,
  medical_info TEXT,
  injury_info TEXT,
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS player (
  id INTEGER NOT NULL,
  details_id INTEGER,
  visible_id VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  gender VARCHAR(5),
  dob DATE,
  weight REAL,
  grade VARCHAR(20),
  locked_status VARCHAR(20),
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS pool (
  id INTEGER NOT NULL ,
  description VARCHAR(512) NOT NULL,
  max_age INTEGER NOT NULL,
  min_age INTEGER NOT NULL,
  max_weight REAL,
  min_weight REAL,
  max_grade VARCHAR(20),
  min_grade VARCHAR(20),
  gender VARCHAR(20),
  match_time INTEGER NOT NULL,
  min_break_time INTEGER NOT NULL,
  golden_score_time INTEGER NOT NULL,
  template_name VARCHAR(512),
  places VARCHAR(5120),
  draw_pools VARCHAR(5120),
  locked_status VARCHAR(20),
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS player_has_pool (
  player_id INTEGER NOT NULL,
  pool_id INTEGER NOT NULL,
  approved BOOL NOT NULL DEFAULT 'false',
  player_pos INTEGER,
  player_pos_2 INTEGER,
  status VARCHAR(32),
  is_locked BOOL,
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(player_id, pool_id)
);

CREATE TABLE IF NOT EXISTS fight (
  id INTEGER NOT NULL,
  pool_id INTEGER NOT NULL,
  player_code1 VARCHAR(16),
  player_code2 VARCHAR(16),
  pos_in_pool INTEGER NOT NULL,
  is_locked BOOL,
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS session (
  id INTEGER NOT NULL,
  session_type VARCHAR(20) NOT NULL,
  name VARCHAR(512),
  mat VARCHAR(128),
  locked_status VARCHAR(20),
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS session_link (
  id INTEGER NOT NULL,
  session_id INTEGER NOT NULL,
  following_id INTEGER NOT NULL,
  link_type VARCHAR(10),
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS session_has_pool (
  session_id INTEGER NOT NULL,
  pool_id INTEGER NOT NULL,
  is_valid BOOL,
  last_updated TIME NOT NULL,
  PRIMARY KEY(session_id, pool_id)
);

CREATE TABLE IF NOT EXISTS session_has_fight (
    fight_id INTEGER NOT NULL,
    session_id INTEGER NOT NULL,
    pos_in_session INTEGER NOT NULL,
    is_valid BOOL,
    last_updated TIME NOT NULL,
    PRIMARY KEY(session_id, fight_id)
);

CREATE TABLE IF NOT EXISTS fight_result (
    id INTEGER NOT NULL,
    fight_id INTEGER NOT NULL,
    player_score1 VARCHAR(50) NOT NULL,
    player_score2 VARCHAR(50) NOT NULL,
    player_id1 INTEGER NOT NULL,
    player_id2 INTEGER NOT NULL,
    duration INTEGER NOT NULL,
    event_log TEXT,
    is_valid BOOL,
    last_updated TIME NOT NULL,
    PRIMARY KEY(id)
);
