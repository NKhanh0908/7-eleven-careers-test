CREATE TABLE IF NOT EXISTS order_item_toppings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    topping_id UUID NOT NULL REFERENCES toppings(id),
    topping_name VARCHAR(100) NOT NULL,
    extra_price NUMERIC(12,0) NOT NULL
);
