# 02 · Certificados e PKI

**[← 01 · Criptografia básica: as quatro ferramentas](01-criptografia-basica.md) · [Sumário](../README.md) · [03 · TLS e mTLS →](03-tls-e-mtls.md)**

> **Analogia:** o certificado é o **documento com selo do cartório**. Contém quem é o Fulano (nome/SAN), a **chave pública** dele, o prazo do selo e a **assinatura do cartório**. A cadeia Tribunal → Cartório → Fulano é a **cadeia de certificação**.

## O que é um certificado X.509

Um arquivo (geralmente PEM ou DER) com:

| Campo | Para que serve | Analogia |
|---|---|---|
| **Subject** | quem é o dono (`CN=...`) | nome no selo |
| **SAN** (Subject Alternative Name) | nomes/IPs válidos (`DNS:api.exemplo.com`) | CPF/nome oficial que o banco confere |
| **Public Key** | chave pública do dono | amostra da assinatura de próprio punho |
| **Issuer** | quem assinou (a CA) | qual cartório carimbou |
| **Validity** | `notBefore` / `notAfter` | prazo do selo |
| **Key Usage / EKU** | para que serve (`serverAuth`, `clientAuth`) | "vale só para este tipo de ato" |
| **Basic Constraints** | `CA:TRUE` se pode emitir outros certificados | "este é um cartório" |
| **Signature** | assinatura da CA sobre tudo acima | o selo em si |

> Hoje clientes modernos validam o **SAN**, não o CN, para o nome do servidor (RFC 6125 📎). Sempre coloque o nome no SAN.

## A cadeia (chain of trust)

```
┌────────────────────────┐   assina   ┌────────────────────────┐   assina   ┌──────────────────────┐
│  CA RAIZ (autoassinada)│──────────▶│  CA INTERMEDIÁRIA      │──────────▶│  CERTIFICADO FOLHA   │
│  está no truststore    │            │  Basic Constraints CA  │            │  CN/SAN=api.x.com    │
│  (o "Tribunal")        │            │  (o "Cartório")        │            │  (o "Fulano")        │
└────────────────────────┘            └────────────────────────┘            └──────────────────────┘
       ▲ confiada de antemão                  ▲ servidor deve ENVIAR                  ▲ servidor envia
```

Regras que explicam 90% dos incidentes:

1. O cliente só confia na **raiz** que já tem no truststore.
2. O servidor deve enviar **folha + intermediárias** (não precisa enviar a raiz).
3. Se faltar a intermediária, alguns clientes "completam" sozinhos e outros (como a JVM, em geral) falham. Resultado: "funciona no browser, quebra no Java".
4. Cada certificado da cadeia precisa estar **válido** (prazo) e ter sido assinado pelo anterior.

### Autoassinado

Um certificado em que Issuer = Subject. A raiz é sempre assim. O problema é usar um autoassinado **como folha** em produção: ninguém tem ele no truststore, então você acaba espalhando exceções. A alternativa é ter uma **CA interna** (raiz + intermediária) e distribuir **a raiz** nos truststores.

## Formatos: o grande "confundidor"

| Formato | Aparência | Contém | Uso típico |
|---|---|---|---|
| **PEM** | texto `-----BEGIN CERTIFICATE-----` | cert, chave ou cadeia (Base64) | Nginx, Kubernetes, Spring (PEM bundle) |
| **DER** | binário | um cert | Windows, alguns appliances |
| **PKCS#12** (`.p12`, `.pfx`) | binário, protegido por senha | chave privada + cert + cadeia | **padrão de keystore do Java desde o Java 9** |
| **JKS** | binário, formato Java antigo | chaves e certs | legado Java |

```bash
# Ver um certificado PEM
openssl x509 -in servidor.crt -noout -text
openssl x509 -in servidor.crt -noout -subject -issuer -dates -ext subjectAltName

# Converter
openssl x509 -in cert.der -inform DER -out cert.pem -outform PEM      # DER -> PEM
openssl pkcs12 -export -inkey servidor.key -in servidor.crt \
        -certfile inter.crt -name servidor -out servidor.p12           # PEM -> PKCS12
openssl pkcs12 -in servidor.p12 -nodes -out tudo.pem                    # PKCS12 -> PEM (cuidado: chave sem senha!)
```

## Montando uma PKI de laboratório

O script [`labs/tls-java/gen-certs.sh`](../labs/tls-java/gen-certs.sh) cria raiz → intermediária → servidor e cliente. Os comandos centrais:

```bash
# 1. Raiz (autoassinada)
openssl req -x509 -newkey rsa:3072 -nodes -keyout raiz.key -out raiz.crt -days 3650 \
  -subj "/CN=Cartorio Raiz (lab)" \
  -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign"

# 2. Intermediária: CSR assinado pela raiz
openssl req -newkey rsa:3072 -nodes -keyout inter.key -out inter.csr -subj "/CN=Cartorio Intermediario (lab)"
openssl x509 -req -in inter.csr -CA raiz.crt -CAkey raiz.key -CAcreateserial -out inter.crt -days 1825 \
  -extfile <(printf "basicConstraints=critical,CA:TRUE,pathlen:0\nkeyUsage=critical,keyCertSign,cRLSign")

# 3. Servidor, com SAN e EKU serverAuth
openssl req -newkey rsa:2048 -nodes -keyout servidor.key -out servidor.csr -subj "/CN=localhost"
openssl x509 -req -in servidor.csr -CA inter.crt -CAkey inter.key -CAcreateserial -out servidor.crt -days 90 \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=serverAuth")
```

Verificar a cadeia:

```bash
openssl verify -CAfile raiz.crt -untrusted inter.crt servidor.crt     # servidor.crt: OK
```

## Revogação e validade

- **Validade curta** (dias/semanas, automatizada) é a prática moderna para certificados de serviço. Veja [cert-manager](10-segredos-e-cofres.md) para renovar automaticamente.
- **CRL** e **OCSP** existem para revogação (RFC 5280 / RFC 6960 📎). Em Java, a checagem de revogação **não vem ligada por padrão** em conexões TLS comuns; ela é habilitada por propriedades de segurança (ex.: `com.sun.security.enableCRLDP`, `ocsp.enable`) ⚠️ confira no JSSE Reference Guide da sua versão.

## Armadilhas e "faça assim"

| Não faça | Faça |
|---|---|
| Colocar o nome só no CN | Colocar **todos** os nomes/IPs no SAN |
| Gerar certificados de 10 anos para serviços | Certificados curtos, renovados automaticamente |
| Exportar chave privada para planilha/e-mail/chat | Gerar a chave **onde ela será usada** e enviar só o CSR |
| Distribuir o certificado folha como "confiança" | Distribuir a **raiz** da CA interna nos truststores |
| Esquecer a intermediária no servidor | Montar o `fullchain` (folha + intermediárias) |
| Misturar PEM/PKCS12 sem saber o que há dentro | Conferir com `openssl x509 -text` / `keytool -list -v` |

## Fontes

- 📎 RFC 5280 (perfil X.509 e validação de caminho) · RFC 6125 (verificação de identidade de serviço) · RFC 8446 §4.4 (mensagens de certificado)
- ✅ Execução real: [`labs/tls-java/gen-certs.sh`](../labs/tls-java/gen-certs.sh)
- ✅ keytool/Java: documentação oficial (ver [14](14-referencias.md))

**[← 01 · Criptografia básica: as quatro ferramentas](01-criptografia-basica.md) · [Sumário](../README.md) · [03 · TLS e mTLS →](03-tls-e-mtls.md)**
