alter table orders add column product_id bigint null after id;

update orders o
inner join options opt on opt.id = o.option_id
set o.product_id = opt.product_id;

alter table orders modify column product_id bigint not null;

alter table orders drop foreign key orders_ibfk_1;
