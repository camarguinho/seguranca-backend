# 00 · Introdução: a história do cartório

**[Sumário](../README.md) · [01 · Criptografia básica: as quatro ferramentas →](01-criptografia-basica.md)**

> **Para quem é:** quem escreve, opera ou dá suporte a serviços backend (Java/Spring/Kubernetes) e precisa **entender** certificados, TLS, tokens e SSO bem o bastante para ensinar outras pessoas e resolver incidentes.

## O problema em uma frase

Em sistemas distribuídos, o tempo todo dois lados que **não se conhecem** precisam decidir se podem confiar um no outro. Este livro trata das ferramentas que a indústria padronizou para isso:

| Pergunta | Ferramenta | Capítulos |
|---|---|---|
| "Esta mensagem foi alterada no caminho?" | hash, HMAC, assinatura | [01](01-criptografia-basica.md) |
| "Este servidor é quem diz ser?" | certificado X.509 + PKI | [02](02-certificados-e-pki.md), [03](03-tls-e-mtls.md) |
| "Em quem a minha JVM confia?" | keystore / truststore / `cacerts` | [04](04-java-keystore-truststore.md) |
| "Por que a conexão falhou?" | diagnóstico de TLS | [05](05-diagnostico-tls.md) |
| "Quem é este chamador e o que ele pode?" | JWT, OAuth 2.0, OIDC | [06](06-jwt.md), [07](07-oauth2-oidc.md) |
| "Quem emite os tokens da empresa?" | Keycloak / RH-SSO | [08](08-keycloak-rhsso.md) |
| "Como minha API valida o token?" | Spring Security | [09](09-spring-security.md) |
| "Onde ficam as chaves e senhas?" | cofres, Secrets | [10](10-segredos-e-cofres.md) |
| "Como tudo isso roda no cluster?" | Spring Boot + Kubernetes | [11](11-spring-boot-kubernetes.md) |

## A analogia que usaremos o livro inteiro: **firma reconhecida em cartório**

Você já levou um documento para **reconhecer firma**. O fluxo real é este:

1. Você assina o documento na frente do tabelião.
2. O cartório coloca um **selo** dizendo "esta assinatura é de Fulano".
3. Quem **recebe** o documento (um banco, por exemplo) não conhece o Fulano. Mas **conhece e confia naquele cartório** (ele está na lista de cartórios que o banco aceita).
4. O cartório, por sua vez, só tem valor porque é **credenciado por um órgão superior** (o Tribunal).

Isso é exatamente uma infraestrutura de chaves públicas (PKI):

```
 TRIBUNAL (raiz)  ──credencia──▶  CARTÓRIO (intermediária)  ──reconhece──▶  FULANO (seu servidor)
  "todo mundo já                    "selo oficial"                          "documento com selo"
   confia em mim"
```

| No cartório | Em TLS / Java | Capítulo |
|---|---|---|
| Fulano | servidor (ou cliente) com um certificado | 02 |
| Assinatura de próprio punho | **chave privada** (nunca sai de você) | 01 |
| Selo do cartório | **assinatura da CA** sobre seu certificado | 02 |
| Nome/CPF escritos no selo | **Subject / SAN** (nome do servidor) | 02 |
| Prazo do selo | **validade** (`notBefore`/`notAfter`) | 02 |
| Cartório | **CA intermediária** | 02 |
| Tribunal | **CA raiz** | 02 |
| Lista de cartórios que o banco aceita | **truststore** (`cacerts` na JVM) | 04 |
| Seus documentos e sua assinatura guardados | **keystore** | 04 |
| "Esse cartório não está na minha lista" | `PKIX path building failed` | 05 |
| Fulano que "reconhece a própria firma" | certificado **autoassinado** | 02 |
| Os dois lados exibem selos | **mTLS** | 03 |

### Onde a analogia falha (e você deve avisar seus liderados)

- O banco **não liga para o cartório** no momento da conferência: ele só confere o selo contra a lista que **já tem**. Por isso a raiz precisa estar **antes** no truststore.
- No TLS, o servidor **prova que possui a chave privada** assinando algo na hora. No papel, não existe equivalente: copiar um documento carimbado não prova nada. No TLS, copiar o certificado (que é público) não adianta sem a chave privada.

## Como ler este livro

- Cada capítulo tem: **analogia → desenho → como funciona → código/comandos → armadilhas → "faça assim" → fontes**.
- Toda recomendação de "não faça" vem com o "faça isto no lugar".
- Selos de confiança nas fontes:
  - ✅ **Lido** na documentação oficial durante a escrita deste livro.
  - 📎 **Citado** (norma/documentação oficial conhecida), mas **não reaberto** na escrita: confira antes de citar em auditoria.
  - ⚠️ **Não verificado**: o detalhe depende da sua versão; teste no seu ambiente.

Veja a lista completa em [14 · Referências](14-referencias.md).

## Laboratórios (tudo aqui foi executado)

Os trechos de saída e erros que aparecem nos capítulos vêm de execuções reais dos laboratórios em [`labs/`](../labs/):

| Laboratório | Para quê | Requisitos |
|---|---|---|
| [`labs/cripto-java`](../labs/cripto-java/) | hash, HMAC, AES-GCM, assinatura RSA | JDK 17+ |
| [`labs/tls-java`](../labs/tls-java/) | PKI de laboratório, servidor/cliente TLS e mTLS, 9 cenários de falha | JDK 17+, `openssl`, `bash` |
| [`labs/jwt-bash`](../labs/jwt-bash/) | montar e validar um JWT só com `openssl` | `openssl`, `bash` |
| [`labs/jwt-java`](../labs/jwt-java/) | validar JWT com `Signature` do JDK (RS256, iss, aud, exp) | JDK 17+ |

```bash
# Primeira vez
cd labs/tls-java && ./gen-certs.sh && ./cenarios.sh
```

> **Acentos aparecendo como `?`?** Execute com `java -Dstdout.encoding=UTF-8 ...` (JDK 19+) ou use um locale UTF-8 (`export LC_ALL=C.UTF-8`).

> **Aviso sobre código de laboratório:** serve para *enxergar* o mecanismo. Em produção use bibliotecas mantidas (Spring Security, Nimbus JOSE, o próprio JSSE), nunca sua própria validação de JWT.

**[Sumário](../README.md) · [01 · Criptografia básica: as quatro ferramentas →](01-criptografia-basica.md)**
