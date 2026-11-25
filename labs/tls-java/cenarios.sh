#!/usr/bin/env bash
# Roda os cenários do capítulo "TLS e mTLS" e mostra o que acontece em cada um.
# Pré-requisitos: JDK 17+ (java no PATH), openssl e ./gen-certs.sh já executado.
set -uo pipefail

C=certs
S=changeit
cliente() { java Cliente.java "$@" 2>&1 | grep -v "Picked up" | head -"${LINHAS:-3}"; }
titulo()  { printf '\n=== %s ===\n' "$1"; }
espera()  { for _ in $(seq 1 40); do (echo > /dev/tcp/127.0.0.1/"$1") 2>/dev/null && return; sleep 0.5; done; }

# Servidor "sem intermediária": envia só a folha, para provar o erro clássico.
openssl pkcs12 -export -inkey $C/servidor.key -in $C/servidor.crt \
  -name servidor -out $C/servidor-sem-inter.p12 -passout "pass:$S"

java Servidor.java $C/servidor.p12 $S 8443 >/dev/null 2>&1 &            P1=$!
java Servidor.java $C/servidor-sem-inter.p12 $S 8445 >/dev/null 2>&1 &  P2=$!
java Servidor.java $C/servidor.p12 $S 8444 $C/truststore.p12 >/dev/null 2>&1 & P3=$!
trap 'kill $P1 $P2 $P3 2>/dev/null' EXIT
espera 8443; espera 8444; espera 8445

titulo "1. TLS comum, cliente com o truststore certo (raiz)"
cliente https://localhost:8443/ $C/truststore.p12 $S

titulo "2. Cliente usando o cacerts do JDK (a CA do laboratório não está lá)"
LINHAS=2 cliente https://localhost:8443/ -

titulo "3. Cliente com truststore que não contém a nossa CA"
LINHAS=2 cliente https://localhost:8443/ $C/truststore-errado.p12 $S

titulo "4. Hostname fora do SAN (127.0.0.2 não está no certificado)"
LINHAS=2 cliente https://127.0.0.2:8443/ $C/truststore.p12 $S

titulo "5. Servidor que esqueceu de enviar a intermediária"
LINHAS=2 cliente https://localhost:8445/ $C/truststore.p12 $S

titulo "6. mTLS: cliente sem certificado"
LINHAS=2 cliente https://localhost:8444/ $C/truststore.p12 $S

titulo "7. mTLS: cliente com certificado"
cliente https://localhost:8444/ $C/truststore.p12 $S $C/cliente.p12 $S

titulo "8. O que o servidor apresenta (openssl)"
openssl s_client -connect localhost:8443 -servername localhost -showcerts </dev/null 2>/dev/null \
  | grep -E "^( [0-9] s:|   i:|Verify return|Verification)" | head -8

titulo "9. O mesmo, só com o keytool"
keytool -printcert -sslserver localhost:8443 2>&1 | grep -E "^(Certificate #|Owner|Issuer|Valid from)" | head -12
