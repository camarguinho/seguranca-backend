#!/usr/bin/env bash
# Gera, do zero, uma PKI de laboratório: CA raiz -> CA intermediária -> certificados
# de servidor e de cliente, mais keystores e truststore no formato PKCS12.
#
# ATENÇÃO: material apenas para estudo. Nunca reutilize estas chaves fora do laboratório
# e nunca versione chaves reais (a pasta certs/ está no .gitignore).
#
# Uso: ./gen-certs.sh [pasta-de-saida]   (padrão: certs)
set -euo pipefail

DIR="${1:-certs}"
SENHA="changeit"            # senha dos .p12 de laboratório

rm -rf "$DIR" && mkdir -p "$DIR" && cd "$DIR"

echo ">> 1/5 CA raiz (o Tribunal que credencia os cartórios)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out raiz.key
openssl req -x509 -new -key raiz.key -sha256 -days 3650 \
  -subj "/CN=Cartorio Raiz (lab)" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -out raiz.crt

echo ">> 2/5 CA intermediária (o cartório que atende no dia a dia)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out inter.key
openssl req -new -key inter.key -subj "/CN=Cartorio Intermediario (lab)" -out inter.csr
openssl x509 -req -in inter.csr -CA raiz.crt -CAkey raiz.key -CAcreateserial \
  -days 1825 -sha256 \
  -extfile <(printf 'basicConstraints=critical,CA:TRUE,pathlen:0\nkeyUsage=critical,keyCertSign,cRLSign\n') \
  -out inter.crt

echo ">> 3/5 Certificado do servidor (SAN: localhost e 127.0.0.1)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out servidor.key
openssl req -new -key servidor.key -subj "/CN=localhost" -out servidor.csr
openssl x509 -req -in servidor.csr -CA inter.crt -CAkey inter.key -CAcreateserial \
  -days 90 -sha256 \
  -extfile <(printf 'basicConstraints=CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectAltName=DNS:localhost,IP:127.0.0.1\n') \
  -out servidor.crt

echo ">> 4/5 Certificado do cliente (para mTLS)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out cliente.key
openssl req -new -key cliente.key -subj "/CN=app-pedidos" -out cliente.csr
openssl x509 -req -in cliente.csr -CA inter.crt -CAkey inter.key -CAcreateserial \
  -days 90 -sha256 \
  -extfile <(printf 'basicConstraints=CA:FALSE\nkeyUsage=critical,digitalSignature\nextendedKeyUsage=clientAuth\n') \
  -out cliente.crt

echo ">> 5/5 Keystores (quem eu sou) e truststore (em quem eu confio)"
# O servidor precisa enviar a folha E a intermediária: -certfile coloca a intermediária na cadeia.
openssl pkcs12 -export -inkey servidor.key -in servidor.crt -certfile inter.crt \
  -name servidor -out servidor.p12 -passout "pass:$SENHA"
openssl pkcs12 -export -inkey cliente.key -in cliente.crt -certfile inter.crt \
  -name cliente -out cliente.p12 -passout "pass:$SENHA"

# Truststore só com a RAIZ. A intermediária chega pelo servidor durante o handshake.
keytool -importcert -alias raiz -file raiz.crt \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$SENHA" -noprompt

# Truststore "errado" de propósito: contém outra CA, para provocar o erro PKIX.
openssl req -x509 -newkey rsa:2048 -nodes -keyout /dev/null -days 30 \
  -subj "/CN=Cartorio Desconhecido (lab)" -out desconhecido.crt 2>/dev/null
keytool -importcert -alias desconhecido -file desconhecido.crt \
  -keystore truststore-errado.p12 -storetype PKCS12 -storepass "$SENHA" -noprompt

rm -f ./*.csr ./*.srl
echo "Pronto em: $(pwd)"
ls -1
