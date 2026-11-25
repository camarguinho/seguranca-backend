# Segurança Backend: certificados, TLS, tokens e SSO (Java · Spring · Kubernetes)

Ebook prático e **lúdico** para quem constrói e opera serviços backend: como funcionam **certificados**, **TLS/mTLS**, **keystore/truststore/`cacerts`**, **JWT**, **OAuth 2.0/OIDC**, **Keycloak/RH-SSO**, **Spring Security**, **segredos** e **Spring Boot no Kubernetes**.

Cada capítulo traz **analogia** (a história da *firma reconhecida em cartório*), **desenho**, **diagramas de sequência**, **código Java**, **comandos bash/Linux**, armadilhas com o "faça assim" e **fontes oficiais**.

> Fontes: apenas normas (IETF, OpenID, NIST) e documentação oficial de produto. Cada fonte é marcada como ✅ lida, 📎 citada ou ⚠️ não verificada. Veja [14 · Referências](capitulos/14-referencias.md).

## Sumário

| # | Capítulo | Tema |
|---|---|---|
| 00 | [Introdução: a história do cartório](capitulos/00-introducao.md) | analogia, mapa do livro, laboratórios |
| 01 | [Criptografia básica](capitulos/01-criptografia-basica.md) | hash, HMAC, AES-GCM, assinatura |
| 02 | [Certificados e PKI](capitulos/02-certificados-e-pki.md) | X.509, cadeia, formatos, PKI de laboratório |
| 03 | [TLS e mTLS](capitulos/03-tls-e-mtls.md) | handshake, mTLS, 9 cenários reais |
| 04 | [Java: keystore, truststore e `cacerts`](capitulos/04-java-keystore-truststore.md) | `keytool`, SSL Bundles |
| 05 | [Diagnóstico de TLS](capitulos/05-diagnostico-tls.md) | `openssl s_client`, `javax.net.debug`, sintomas |
| 06 | [JWT](capitulos/06-jwt.md) | anatomia, JWKS, validação, RFC 8725 |
| 07 | [OAuth 2.0 e OpenID Connect](capitulos/07-oauth2-oidc.md) | fluxos, PKCE, client credentials |
| 08 | [Keycloak e RH-SSO](capitulos/08-keycloak-rhsso.md) | realms, clients, roles (⚠️ ver aviso) |
| 09 | [Spring Security](capitulos/09-spring-security.md) | resource server, roles, `aud`, RestClient |
| 10 | [Segredos, chaves e cofres](capitulos/10-segredos-e-cofres.md) | Secrets, cert-manager, Vault, trust-manager |
| 11 | [Spring Boot no Kubernetes](capitulos/11-spring-boot-kubernetes.md) | arquitetura, bundles, probes, ingress |
| 12 | [Runbooks, checklists e aula](capitulos/12-runbooks-checklists-aula.md) | tabela de incidentes, aula de 30 min |
| 13 | [Glossário](capitulos/13-glossario.md) | termos e analogias |
| 14 | [Referências](capitulos/14-referencias.md) | fontes e transparência |

### Trilhas sugeridas

- **Estou em incidente agora:** [05](capitulos/05-diagnostico-tls.md) → [12](capitulos/12-runbooks-checklists-aula.md).
- **Quero entender do zero:** 00 → 01 → 02 → 03 → 04 → 06 → 07.
- **Vou ensinar o time:** [12 (aula de 30 min)](capitulos/12-runbooks-checklists-aula.md) + [`labs/`](labs/).
- **Vou publicar no Kubernetes:** [10](capitulos/10-segredos-e-cofres.md) → [11](capitulos/11-spring-boot-kubernetes.md).

## Laboratórios (executáveis)

| Pasta | Conteúdo |
|---|---|
| [`labs/cripto-java`](labs/cripto-java/) | `java Cripto.java`: hash, HMAC, AES-GCM, RSA |
| [`labs/tls-java`](labs/tls-java/) | `./gen-certs.sh && ./cenarios.sh`: PKI, servidor/cliente TLS e mTLS, 9 cenários |
| [`labs/jwt-bash`](labs/jwt-bash/) | `./jwt-bash.sh`: JWT só com `openssl` |
| [`labs/jwt-java`](labs/jwt-java/) | `java JwtNaMao.java`: validação RS256 com o JDK |

Requisitos: JDK 17+, `openssl`, `bash`. Para acentos corretos: `java -Dstdout.encoding=UTF-8 ...` (JDK 19+) ou locale UTF-8.

> Material didático: os laboratórios não substituem bibliotecas maduras em produção.

## Contribuindo

Correções são bem-vindas com **a fonte oficial** que as sustenta.
