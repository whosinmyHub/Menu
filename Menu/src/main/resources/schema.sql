SET COLLATION ENGLISH STRENGTH PRIMARY;

CREATE TABLE Menu (
	food_type VARCHAR (30) NOT NULL,
	food_name VARCHAR (255),
	
	PRIMARY KEY (food_name)
);

CREATE TABLE Tables (
	table_id int AUTO_INCREMENT,
	
	PRIMARY KEY (table_id)
);

 -- Each table has the same order_id, 
 -- but each order in that table that is of 
 -- differing food names or quantities is a seperate submission
CREATE TABLE Orders (
	table_id int,
	food_name VARCHAR (255),
	quantity INT NOT NULL DEFAULT 1,
	
	PRIMARY KEY (table_id, food_name),
	FOREIGN KEY (food_name) REFERENCES Menu,
	FOREIGN KEY (table_id) REFERENCES Tables
);


INSERT INTO Menu 
VALUES 
('appetizer', 'breadsticks'),
('appetizer', 'olives'),
('appetizer', 'grapes'),
('entree', 'pasta with meatballs'),
('entree', 'chicken tikka masala'),
('entree', 'sushi'),
('dessert', 'chocolate cake'),
('dessert', 'lava cake'),
('dessert', 'fruit cake');