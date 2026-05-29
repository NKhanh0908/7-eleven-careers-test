CREATE TABLE IF NOT EXISTS category_toppings (
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    topping_id UUID NOT NULL REFERENCES toppings(id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, topping_id)
);
