# 14 · Referências e transparência de fontes

**[← 13 · Glossário](13-glossario.md) · [Sumário](../README.md)**

Este livro usa **apenas literatura técnica oficial**: normas (IETF, OpenID Foundation, NIST) e documentação dos projetos/fornecedores. Não há blogs.

## Legenda

- ✅ **Lido** durante a escrita: conteúdo consultado diretamente e usado para checar afirmações.
- 📎 **Citado**: norma ou documentação oficial conhecida e usada como referência, mas **não reaberta** na escrita (o ambiente de autoria não conseguia acessar esses sites). Os detalhes vêm do conhecimento do autor e devem ser conferidos antes de uso em auditoria.
- ⚠️ **Não verificado**: ponto específico que depende da versão do seu produto.

## Normas e especificações

| Fonte | Selo | Usada em |
|---|---|---|
| RFC 5280 — Internet X.509 PKI Certificate and CRL Profile | 📎 | 02 |
| RFC 6125 — Verificação de identidade de serviço em TLS | 📎 | 02 |
| RFC 6960 — OCSP | 📎 | 02 |
| RFC 8446 — TLS 1.3 · RFC 5246 — TLS 1.2 | 📎 | 03, 05 |
| RFC 2104 — HMAC · RFC 8017 — PKCS#1 | 📎 | 01 |
| NIST SP 800-38D — AES-GCM · FIPS 180-4 — SHA-2 | 📎 | 01 |
| RFC 7515 (JWS) · 7516 (JWE) · 7517 (JWK) · 7518 (JWA) · 7519 (JWT) | 📎 | 06 |
| **RFC 8725 — JSON Web Token Best Current Practices** | 📎 | 06, 09 |
| RFC 6749 — OAuth 2.0 · RFC 6750 — Bearer Token | 📎 | 07, 09 |
| RFC 7636 — PKCE | 📎 | 07 |
| **RFC 9700 — Best Current Practice for OAuth 2.0 Security** | 📎 | 07 |
| RFC 7662 — Token Introspection · RFC 8693 — Token Exchange · RFC 8414 — AS Metadata | 📎 | 07 |
| OpenID Connect Core 1.0 e Discovery 1.0 (openid.net/specs) | 📎 | 07, 08 |

Os RFCs ficam em `https://www.rfc-editor.org/rfc/rfcNNNN` (substitua `NNNN`).

## Documentação de produto

| Fonte | Selo | Usada em |
|---|---|---|
| Spring Boot Reference — *SSL* (bundles JKS/PEM, *Reloading SSL bundles*) | ✅ | 04, 11 |
| Spring Boot Reference — *Embedded Web Servers*, *Actuator: Monitoring* (porta de gerência e SSL próprio), *REST Clients* (`RestClientSsl`, `WebClientSsl`), *Externalized Configuration* (`configtree`) | ✅ | 10, 11 |
| Spring Security Reference — *OAuth2 Resource Server (JWT)*, *Opaque Token*, *OAuth2 Client: Authorized Clients*, *Testing OAuth 2.0* | ✅ | 07, 09 |
| Spring Security Reference — *X.509 Authentication* (versão reativa lida) | ✅ parcial | 09 |
| JDK — `keytool` (manual oficial) e `java` | ✅ | 04, 05 |
| JSSE Reference Guide (Oracle/OpenJDK): ordem de truststore, `javax.net.ssl.*`, `javax.net.debug` | 📎 | 04, 05 |
| Kubernetes — *Secrets*, *Good practices for Secrets*, *Encrypt data at rest*, *Ingress (TLS)*, *Liveness/Readiness/Startup Probes* | ✅ | 10, 11 |
| cert-manager — *Certificate*, *CA issuer*, *csi-driver*, *trust-manager* | ✅ | 10, 11 |
| External Secrets Operator · HashiCorp Vault (README/documentação) | ✅ | 10 |
| Keycloak Documentation (keycloak.org/documentation) | ⚠️ não aberta | 08 |
| Red Hat Single Sign-On / Red Hat build of Keycloak (docs.redhat.com) | ⚠️ não aberta | 08 |

> **Atenção ao Capítulo 08:** a documentação do Keycloak/RH-SSO não pôde ser acessada. O capítulo traz conceitos gerais e comandos para você **conferir no seu servidor**. Valide menus, caminhos (`/auth`) e a relação RH-SSO ↔ Keycloak com a documentação da versão que a sua empresa opera, antes de ensinar.

## O que **não** foi compilado

- Os trechos de **Spring** (caps. 09 e 11) seguem a documentação oficial, mas não foram compilados nem executados (o Maven Central não estava acessível).
- O que **foi executado** de verdade: [`labs/cripto-java`](../labs/cripto-java/), [`labs/tls-java`](../labs/tls-java/), [`labs/jwt-bash`](../labs/jwt-bash/), [`labs/jwt-java`](../labs/jwt-java/) (JDK 21, OpenSSL 3).
- Os diagramas Mermaid foram **validados sintaticamente** com o parser do Mermaid.

## Como propor correções

Abra uma *issue* ou *pull request* indicando o capítulo, o trecho e a **fonte oficial** que sustenta a correção.

**[← 13 · Glossário](13-glossario.md) · [Sumário](../README.md)**
