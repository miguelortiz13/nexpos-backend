#!/usr/bin/env bash
# Prueba de humo del flujo de negocio de NexPOS contra una API en marcha.
#   ./scripts/smoke-test.sh http://localhost:8088 admin <contraseña>
# Crea un producto, abre un turno de caja, registra una venta, descarga la
# factura PDF, anula la venta (nota crédito) y verifica el stock.
set -euo pipefail

BASE=${1:?URL base de la API}
USER=${2:-admin}
PASS=${3:?contraseña}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

ok()   { echo "  ✔ $*"; }
fail() { echo "  ✘ $*" >&2; exit 1; }
api()  { # api METHOD PATH [JSON]
  local method=$1 path=$2 body=${3:-}
  local args=(-sS -o "$WORK/body" -w '%{http_code}' -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN")
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' -d "$body")
  CODE=$(curl "${args[@]}")
  BODY=$(cat "$WORK/body")
}

echo "NexPOS smoke test → $BASE"

curl -sf "$BASE/actuator/health" | grep -q '"status":"UP"' && ok "health UP" || fail "health"

TOKEN=$(curl -sf -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" | jq -r '.token // .accessToken // empty')
[[ -n "$TOKEN" ]] && ok "login" || fail "login"

SKU="SMOKE$(date +%s)"
api POST /api/productos "{\"codigoBarras\":\"$SKU\",\"nombre\":\"Prueba humo ñandú\",\"marca\":\"QA\",\"precio\":2500,\"cantidad\":10,\"categoria\":\"QA\",\"costPrice\":1500,\"minStock\":1}"
[[ $CODE =~ ^20 ]] || fail "crear producto ($CODE): $BODY"
PID=$(jq -r .id <<<"$BODY"); ok "producto $PID"

api GET "/api/productos/codigo/$SKU"
[[ $CODE == 200 && $(jq -r .nombre <<<"$BODY") == "Prueba humo ñandú" ]] && ok "búsqueda por código de barras (unicode)" || fail "búsqueda ($CODE): $BODY"

api GET /api/cash-shifts/active
if [[ $CODE != 200 || -z "$BODY" || "$BODY" == "null" ]]; then
  api POST /api/cash-shifts/open '{"initialAmount":50000,"notes":"smoke"}'
  [[ $CODE =~ ^20 ]] || fail "abrir turno ($CODE): $BODY"
fi
ok "turno de caja abierto"

api POST /api/sales "{\"customerId\":1,\"customerName\":\"Consumidor Final\",\"customerDoc\":\"222222222222\",\"paymentMethod\":\"EFECTIVO\",\"amountPaid\":10000,\"cashAmount\":5000,\"items\":[{\"productId\":$PID,\"productName\":\"Prueba humo\",\"quantity\":2,\"price\":2500}]}"
[[ $CODE =~ ^20 ]] || fail "venta ($CODE): $BODY"
SID=$(jq -r .id <<<"$BODY"); ok "venta $SID por $(jq -r .totalAmount <<<"$BODY")"

api GET "/api/productos/$PID"
[[ $(jq -r .cantidad <<<"$BODY") == 8 ]] && ok "stock descontado (10 → 8)" || fail "stock tras venta: $BODY"

curl -sf -o "$WORK/inv.pdf" "$BASE/api/sales/$SID/invoice" -H "Authorization: Bearer $TOKEN" && head -c4 "$WORK/inv.pdf" | grep -q '%PDF' && ok "factura PDF" || fail "factura PDF"

api POST "/api/sales/$SID/annul" '{"reason":"Prueba de humo","conceptCode":"2","refundCash":true}'
[[ $CODE =~ ^20 ]] || fail "anular ($CODE): $BODY"
ok "venta anulada"

api GET "/api/sales/$SID/credit-note"
[[ $CODE == 200 ]] && ok "nota crédito $(jq -r .creditNoteNumber <<<"$BODY")" || fail "nota crédito ($CODE): $BODY"

api GET "/api/productos/$PID"
[[ $(jq -r .cantidad <<<"$BODY") == 10 ]] && ok "stock restituido (8 → 10)" || fail "stock tras anulación: $BODY"

api DELETE "/api/productos/$PID" || true
echo "OK: flujo completo"
