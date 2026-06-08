delete duplicated
from wish duplicated
inner join wish kept
    on kept.member_id = duplicated.member_id
    and kept.product_id = duplicated.product_id
    and kept.id < duplicated.id;

alter table wish add constraint uk_wish_member_product unique (member_id, product_id);
