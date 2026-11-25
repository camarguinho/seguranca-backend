#!/usr/bin/env bash
# Monta, assina (RS256) e valida um JWT usando só openssl e shell.
# Serve para ver que um JWT é "texto + assinatura", sem mágica.
#
# ATENÇÃO: material didático. Em produção use uma biblioteca madura.
set -euo pipefail

b64url()     { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
b64url_dec() {  # Base64URL -> bytes (restaura o padding que o JWT remove)
  local s; s=$(printf '%s' "$1" | tr '_-' '/+')
  case $(( ${#s} % 4 )) in 2) s="$s==";; 3) s="$s=";; esac
  printf '%s' "$s" | openssl base64 -d -A
}

TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP/priv.pem" 2>/dev/null
openssl pkey -in "$TMP/priv.pem" -pubout -out "$TMP/pub.pem"

AGORA=$(date +%s)
HEADER='{"alg":"RS256","typ":"JWT","kid":"chave-lab"}'
PAYLOAD=$(printf '{"iss":"https://idp.exemplo.com/realms/loja","sub":"u-123","aud":"api-pedidos","scope":"pedidos:ler","iat":%s,"exp":%s}' "$AGORA" "$((AGORA + 300))")

H=$(printf '%s' "$HEADER"  | b64url)
P=$(printf '%s' "$PAYLOAD" | b64url)
S=$(printf '%s' "$H.$P" | openssl dgst -sha256 -sign "$TMP/priv.pem" | b64url)
JWT="$H.$P.$S"

echo "== JWT (3 partes)"; echo "$JWT" | cut -c1-110; echo "..."
echo; echo "== Corpo decodificado (qualquer um lê)"; b64url_dec "$P"; echo

echo; echo "== Validando a assinatura com a chave pública"
b64url_dec "$S" > "$TMP/sig.bin"
printf '%s' "$H.$P" | openssl dgst -sha256 -verify "$TMP/pub.pem" -signature "$TMP/sig.bin"

echo; echo "== Adulterando o corpo (escopo vira pedidos:apagar)"
P2=$(printf '%s' "${PAYLOAD/pedidos:ler/pedidos:apagar}" | b64url)
printf '%s' "$H.$P2" | openssl dgst -sha256 -verify "$TMP/pub.pem" -signature "$TMP/sig.bin" || true
