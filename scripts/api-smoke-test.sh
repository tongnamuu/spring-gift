#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-spring_gift}"
MYSQL_USER="${MYSQL_USER:-gift}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-gift}"
CATEGORY_ID="${CATEGORY_ID:-1}"

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

require_command() {
    if ! command_exists "$1"; then
        echo "missing required command: $1" >&2
        exit 1
    fi
}

require_command curl
require_command jq
require_command docker

tmp_base="${TMPDIR:-/tmp}"
tmp_dir="$(mktemp -d "${tmp_base%/}/spring-gift-api.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

failures=()

json_string() {
    jq -Rn --arg value "$1" '$value'
}

summarize_body() {
    local file="$1"

    if [ ! -s "$file" ]; then
        printf 'null'
        return
    fi

    if jq -e . "$file" >/dev/null 2>&1; then
        jq -c '
            if type == "object" and has("token") then
                {token:(.token[0:20] + "...")}
            elif type == "object" and has("content") then
                {
                    contentSize:(.content | length),
                    firstContent:(.content[0] // null),
                    totalElements:.totalElements
                }
            elif type == "array" then
                {listSize:length, first:(.[0] // null)}
            else
                .
            end
        ' "$file"
    else
        jq -R -s -c '.' "$file"
    fi
}

record() {
    local name="$1"
    local expected="$2"
    local actual="$3"
    local file="$4"
    local body

    body="$(summarize_body "$file")"
    printf '{"name":%s,"expected":%s,"actual":%s,"body":%s}\n' \
        "$(json_string "$name")" \
        "$(json_string "$expected")" \
        "$(json_string "$actual")" \
        "$body"

    if [ "$expected" != "-" ] && [ "$actual" != "$expected" ]; then
        failures+=("$name expected=$expected actual=$actual")
    fi
}

request() {
    local name="$1"
    local expected="$2"
    local method="$3"
    local path="$4"
    local body="${5:-}"
    local token="${6:-}"
    local out="$tmp_dir/$(printf '%s' "$name" | tr -cs 'a-zA-Z0-9' '_').out"
    local args=(-sS -o "$out" -w "%{http_code}" -X "$method")

    if [ -n "$body" ]; then
        args+=(-H "Content-Type: application/json" -d "$body")
    fi

    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi

    local code
    code="$(curl "${args[@]}" "$BASE_URL$path")"
    record "$name" "$expected" "$code" "$out"
}

mysql_scalar() {
    local sql="$1"

    docker compose exec -T "$MYSQL_SERVICE" mysql \
        "-u$MYSQL_USER" "-p$MYSQL_PASSWORD" \
        --batch --skip-column-names "$MYSQL_DATABASE" \
        -e "$sql" 2>/dev/null | tr -d '\r'
}

echo "# API smoke test"
echo "# base_url=$BASE_URL"
echo "# mysql_service=$MYSQL_SERVICE database=$MYSQL_DATABASE"

health_out="$tmp_dir/health.out"
health_code="$(curl -sS -o "$health_out" -w "%{http_code}" "$BASE_URL/api/categories")"
record "GET /api/categories health" "200" "$health_code" "$health_out"

epoch="$(date +%s)"
epoch_tail="${epoch: -6}"
random_tail="$(printf '%05d' "$((RANDOM % 100000))")"
suffix="${epoch_tail}${random_tail}"

email="api-order-${suffix}@example.com"
password="password123"
product_name="api${suffix}"
option_name="option${suffix}"

request "GET /api/wishes without auth" "401" "GET" "/api/wishes"

request "GET /api/orders without auth" "401" "GET" "/api/orders"

register_body="{\"email\":\"$email\",\"password\":\"$password\"}"
request "POST /api/members/register" "200" "POST" "/api/members/register" "$register_body"
token="$(jq -r '.token' "$tmp_dir/POST_api_members_register_.out")"

request "POST /api/members/login" "200" "POST" "/api/members/login" "$register_body"

member_id="$(mysql_scalar "select id from member where email='$email'")"
if [ -z "$member_id" ]; then
    echo "failed to find created member id for email=$email" >&2
    exit 1
fi

charge_out="$tmp_dir/admin_charge.out"
charge_code="$(curl -sS -o "$charge_out" -w "%{http_code}" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "amount=1000" \
    "$BASE_URL/admin/members/$member_id/charge-point")"
record "POST /admin/members/{id}/charge-point" "302" "$charge_code" "$charge_out"

product_body="{\"name\":\"$product_name\",\"price\":1000,\"imageUrl\":\"https://example.com/api-product.png\",\"categoryId\":$CATEGORY_ID}"
request "POST /api/products" "201" "POST" "/api/products" "$product_body"
product_id="$(jq -r '.id' "$tmp_dir/POST_api_products_.out")"

option_body="{\"name\":\"$option_name\",\"quantity\":2}"
request "POST /api/products/{id}/options" "201" "POST" "/api/products/$product_id/options" "$option_body"
option_id="$(jq -r '.id' "$tmp_dir/POST_api_products_id_options_.out")"

wish_body="{\"productId\":$product_id}"
request "POST /api/wishes" "201" "POST" "/api/wishes" "$wish_body" "$token"
wish_id="$(jq -r '.id' "$tmp_dir/POST_api_wishes_.out")"

request "POST /api/wishes duplicate" "200" "POST" "/api/wishes" "$wish_body" "$token"
request "GET /api/wishes before order" "200" "GET" "/api/wishes" "" "$token"

order_success_body="{\"optionId\":$option_id,\"quantity\":1,\"message\":\"api order success\"}"
request "POST /api/orders success" "201" "POST" "/api/orders" "$order_success_body" "$token"
order_id="$(jq -r '.id' "$tmp_dir/POST_api_orders_success_.out")"

request "GET /api/orders after success" "200" "GET" "/api/orders" "" "$token"
request "GET /api/wishes after order" "200" "GET" "/api/wishes" "" "$token"

order_fail_body="{\"optionId\":$option_id,\"quantity\":1,\"message\":\"api order should fail\"}"
request "POST /api/orders point shortage" "400" "POST" "/api/orders" "$order_fail_body" "$token"

request "GET /api/products/{id} after failed order" "200" "GET" "/api/products/$product_id"

order_count="$(mysql_scalar "select count(*) from orders where member_id = $member_id and product_id = $product_id and option_id = $option_id")"
order_count_out="$tmp_dir/order_count.out"
printf '%s' "$order_count" > "$order_count_out"
record "DB order count after failed order" "1" "$order_count" "$order_count_out"

request "DELETE /api/wishes/{id}" "204" "DELETE" "/api/wishes/$wish_id" "" "$token"
request "GET /api/wishes after delete" "200" "GET" "/api/wishes" "" "$token"

echo "STATE $(jq -n -c \
    --arg email "$email" \
    --arg memberId "$member_id" \
    --arg productId "$product_id" \
    --arg optionId "$option_id" \
    --arg wishId "$wish_id" \
    --arg orderId "$order_id" \
    '{email:$email, memberId:$memberId, productId:$productId, optionId:$optionId, wishId:$wishId, orderId:$orderId}')"

if [ "${#failures[@]}" -gt 0 ]; then
    echo
    echo "FAILED"
    printf ' - %s\n' "${failures[@]}"
    exit 1
fi

echo
echo "PASSED"
