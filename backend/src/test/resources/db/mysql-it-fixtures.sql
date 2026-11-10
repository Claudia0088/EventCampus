USE eventcampus_it;
INSERT INTO roles(id_role,name,description) VALUES
(1,'USER','Fictitious student'),(2,'MODERATOR','Fictitious moderator'),(3,'ADMIN','Fictitious admin');
-- Not login credentials. Login-test accounts are generated separately with BCrypt.
INSERT INTO users(id_user,first_name,last_name,email,password,status,id_role) VALUES
(1,'Camille','Demo','student@example.invalid','!disabled-fixture!','ACTIVE',1),
(2,'Morgan','Demo','moderator@example.invalid','!disabled-fixture!','ACTIVE',2),
(3,'Alex','Demo','admin@example.invalid','!disabled-fixture!','ACTIVE',3);
INSERT INTO categories(id_category,name,description) VALUES (1,'Formation','Fictitious category');
INSERT INTO events(id_event,title,event_date,event_time,location,capacity,status,id_creator,id_category) VALUES
(1,'Atelier fictif',CURRENT_DATE(),'14:00:00','Salle fictive',2,'PUBLIE',1,1),
(2,'Atelier fictif demain',DATE_ADD(CURRENT_DATE(),INTERVAL 1 DAY),'14:00:00','Salle fictive',2,'PUBLIE',1,1),
(3,'Proposition fictive',DATE_ADD(CURRENT_DATE(),INTERVAL 2 DAY),'14:00:00','Salle fictive',2,'EN_ATTENTE',1,1);