# 13 · Glossário

**[← 12 · Runbooks, checklists e a aula de 30 minutos](12-runbooks-checklists-aula.md) · [Sumário](../README.md) · [14 · Referências e transparência de fontes →](14-referencias.md)**

> **Analogia:** o **índice remissivo** do manual do cartório.

| Termo | Definição curta | Analogia | Cap. |
|---|---|---|---|
| **AEAD / AES-GCM** | cifra que protege sigilo **e** integridade | cofre com lacre | 01 |
| **Access token** | credencial curta que a API aceita | crachá do dia | 07 |
| **Audience (`aud`)** | para quem o token foi emitido | andar permitido no crachá | 06 |
| **Authorization Code + PKCE** | fluxo padrão para usuários | pedir o crachá na recepção com um código secreto | 07 |
| **Bearer** | "quem porta, usa": token no cabeçalho `Authorization` | crachá solto: quem tem, entra | 07 |
| **CA (Certificate Authority)** | entidade que assina certificados | cartório | 02 |
| **CA raiz** | CA autoassinada, ponto de confiança | Tribunal | 02 |
| **CA intermediária** | CA assinada pela raiz, que emite folhas | cartório credenciado | 02 |
| **`cacerts`** | truststore padrão do JDK | lista de cartórios de fábrica | 04 |
| **Certificado X.509** | chave pública + identidade + assinatura da CA | documento com selo | 02 |
| **Chave privada / pública** | par; privada assina, pública confere | assinatura de próprio punho / amostra | 01 |
| **Client Credentials** | fluxo serviço→serviço | crachá de máquina | 07 |
| **CN / SAN** | nome no assunto / lista de nomes válidos | nome no selo | 02 |
| **CRL / OCSP** | mecanismos de revogação | lista de selos cancelados | 02 |
| **CSR** | pedido de certificado (chave pública + nome) | requerimento ao cartório | 02 |
| **EKU** | uso permitido (`serverAuth`, `clientAuth`) | "vale só para este tipo de ato" | 02 |
| **Handshake** | negociação inicial do TLS | conferência na mesa do banco | 03 |
| **HMAC** | hash com segredo compartilhado | lacre com senha combinada | 01 |
| **ID token** | JWT do OIDC que diz quem autenticou | comprovante de identidade para o cliente | 07 |
| **Introspecção** | a API pergunta ao AS se o token vale | ligar para a recepção | 07 |
| **JKS / PKCS12** | formatos de keystore Java | tipos de pasta | 04 |
| **JWE / JWS / JWT** | cifrado / assinado / formato de token | crachá lacrado / crachá com selo | 06 |
| **JWKS / JWK / `kid`** | conjunto de chaves públicas / uma chave / seu identificador | modelo do selo publicado | 06 |
| **Keystore** | onde ficam suas chaves privadas e certificados | sua pasta pessoal | 04 |
| **mTLS** | TLS com autenticação dos dois lados | ambos exibem selos | 03 |
| **OIDC** | camada de identidade sobre OAuth 2.0 | recepção que também diz quem você é | 07 |
| **PEM / DER** | texto Base64 / binário | cópia digitada / escaneada | 02 |
| **PKCE** | prova de que quem troca o `code` é quem o pediu | código secreto do protocolo | 07 |
| **PKIX path building** | construção da cadeia até uma raiz confiável | rastrear selo → cartório → Tribunal | 05 |
| **Realm / Client** | espaço de identidade / aplicação no Keycloak | prédio / empresa | 08 |
| **Refresh token** | credencial longa para obter novos access tokens | cartão para renovar o crachá | 07 |
| **Resource server** | API que exige o token | segurança do andar | 07, 09 |
| **Scope** | permissão pedida no token | andares autorizados | 07 |
| **SNI** | nome do servidor enviado no ClientHello | "quero falar com o cartório X" | 05 |
| **Truststore** | quem a JVM aceita como CA | lista de cartórios aceitos | 04 |
| **TLS** | protocolo de canal seguro | sala fechada com conferência de documentos | 03 |

**[← 12 · Runbooks, checklists e a aula de 30 minutos](12-runbooks-checklists-aula.md) · [Sumário](../README.md) · [14 · Referências e transparência de fontes →](14-referencias.md)**
