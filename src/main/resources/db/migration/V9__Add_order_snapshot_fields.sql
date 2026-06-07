alter table orders
    add column product_name varchar(255) null after member_id,
    add column option_name varchar(50) null after product_name,
    add column unit_price int null after option_name,
    add column product_image_url varchar(255) null after unit_price;

update orders o
left join product p on p.id = o.product_id
left join options opt on opt.id = o.option_id
set o.product_name = coalesce(p.name, '삭제된 상품'),
    o.option_name = coalesce(opt.name, '삭제된 옵션'),
    o.unit_price = coalesce(p.price, 0),
    o.product_image_url = coalesce(p.image_url, '');

alter table orders
    modify column product_name varchar(255) not null,
    modify column option_name varchar(50) not null,
    modify column unit_price int not null,
    modify column product_image_url varchar(255) not null;
