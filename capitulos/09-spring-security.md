# 09 · Spring Security: API que valida tokens

**[← 08 · Keycloak e RH-SSO](08-keycloak-rhsso.md) · [Sumário](../README.md) · [10 · Segredos, chaves e cofres →](10-segredos-e-cofres.md)**

> **Analogia:** o **segurança do andar** (resource server). Não conhece ninguém: lê o crachá, confere o selo com o modelo publicado pela recepção (JWKS), verifica prazo e andar (`exp`, `aud`) e libera ou barra (401/403).

> ⚠️ **Os trechos de Spring deste capítulo foram extraídos/adaptados da documentação oficial, mas não foram compilados** (o Maven Central estava inacessível no ambiente de autoria). Compile e teste no seu projeto.

## 401 ou 403?

| Código | Significa | Exemplo |
|---|---|---|
| **401 Unauthorized** | "não sei quem você é / token inválido" | sem token, expirado, assinatura ruim, `iss`/`aud` errados |
| **403 Forbidden** | "sei quem é, mas não pode" | token válido sem a authority necessária |

## Dependências

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## Configuração mínima (✅ doc oficial)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://sso.suaempresa.com/realms/meu-realm
```

O que o Spring faz com isso (resumo da doc ✅):

1. No startup, consulta o **endpoint de metadados** do emissor (`.well-known/...`) para achar o `jwks_uri`.
2. Baixa as chaves públicas e as mantém em cache.
3. Para cada requisição com `Authorization: Bearer ...`, valida a assinatura e o **emissor/prazo**.
4. Converte `scope`/`scp` em authorities com prefixo **`SCOPE_`** (✅ doc).

Se o emissor não expõe metadados, ou se a API precisa subir sem o IdP no ar, informe também `jwk-set-uri` (✅ doc):

```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: https://idp.exemplo.com
  jwk-set-uri: https://idp.exemplo.com/.well-known/jwks.json
```

> **Algoritmo:** por padrão o `NimbusJwtDecoder` aceita **RS256** (✅ doc). Se seu emissor usa outro (ex.: ES256), configure explicitamente `jws-algorithms`. Isso aplica a regra de ouro do cap. 06: o algoritmo vem da **configuração**.

## Cadeia de segurança

```java
@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/pedidos/**").hasAuthority("SCOPE_pedidos:ler")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.disable());   // API stateless com Bearer; NÃO desabilite CSRF em apps com sessão/cookie
        return http.build();
    }
}
```

Em método:

```java
@PreAuthorize("hasAuthority('SCOPE_pedidos:ler')")
@GetMapping("/pedidos/{id}")
Pedido buscar(@PathVariable String id) { ... }
```

## Validar também `aud` (audiência)

Por padrão, o decoder valida assinatura e tempo (e `iss` quando usa `issuer-uri`). **A audiência você adiciona.** Padrão de mercado e consistente com a doc (uso de `OAuth2TokenValidator` + `DelegatingOAuth2TokenValidator`):

```java
@Bean
JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
    NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);   // usa descoberta + JWKS

    OAuth2TokenValidator<Jwt> comIssuer = JwtValidators.createDefaultWithIssuer(issuer);
    OAuth2TokenValidator<Jwt> comAud = jwt ->
        jwt.getAudience() != null && jwt.getAudience().contains("api-pedidos")
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "aud inválida", null));

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(comIssuer, comAud));
    return decoder;
}
```

## Mapear roles do Keycloak para authorities

Por padrão só `scope`/`scp` vira authority. Se suas permissões estão em `realm_access.roles` ou `resource_access.<client>.roles` (⚠️ confirme no seu token, cap. 08), converta:

```java
@Bean
JwtAuthenticationConverter conversor() {
    JwtGrantedAuthoritiesConverter escopos = new JwtGrantedAuthoritiesConverter();   // SCOPE_...

    JwtAuthenticationConverter c = new JwtAuthenticationConverter();
    c.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> r = new ArrayList<>(escopos.convert(jwt));
        Object realm = jwt.getClaimAsMap("realm_access");
        if (realm instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> roles) {
            roles.forEach(x -> r.add(new SimpleGrantedAuthority("ROLE_" + x)));
        }
        return r;
    });
    return c;
}
// uso: .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor())))
```

## Token opaco (introspecção) ✅

```yaml
spring.security.oauth2.resourceserver.opaquetoken:
  introspection-uri: https://idp.exemplo.com/introspect
  client-id: api-pedidos
  client-secret: ${INTROSPECT_SECRET}      # segredo vem de Secret, não do Git
```

```java
.oauth2ResourceServer(o -> o.opaqueToken(Customizer.withDefaults()))
```

## Chamando outra API com token (service-to-service) ✅

A doc oficial descreve o `OAuth2ClientHttpRequestInterceptor` para `RestClient`, que obtém e **renova** o token (Client Credentials) e o coloca no `Authorization`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          pedidos-para-estoque:
            provider: sso
            client-id: svc-pedidos
            client-secret: ${CLIENT_SECRET}
            authorization-grant-type: client_credentials
            scope: estoque:ler
        provider:
          sso:
            issuer-uri: https://sso.suaempresa.com/realms/meu-realm
```

```java
@Configuration
public class RestClientConfig {
    @Bean
    RestClient restClient(OAuth2AuthorizedClientManager gerente) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(gerente);
        return RestClient.builder().requestInterceptor(interceptor).build();
    }
}
```

> Para um cliente que fala com um servidor que usa **CA interna**, injete um SSL Bundle no `RestClient` (✅ doc: `RestClientSsl`, ver cap. 11) e o JWKS/token endpoint também precisam ser acessíveis com truststore adequado.

## mTLS com Spring Security (X.509) ⚠️

Para autenticar **o chamador pelo certificado** (em vez de token):

```yaml
server:
  ssl:
    bundle: servidor
    client-auth: need
```

```java
http.x509(x -> x.subjectPrincipalRegex("CN=(.*?)(?:,|$)"))   // extrai o usuário do CN do certificado
    .authorizeHttpRequests(a -> a.anyRequest().authenticated());
```

Nesse modelo o TLS **já autenticou** o cliente, e o Spring só traduz o certificado em `Authentication`. A doc oficial lida (✅ *X.509 Authentication*, versão reativa) confirma que o extrator padrão usa o **CN** do certificado; as propriedades `server.ssl.client-auth` e `subjectPrincipalRegex` acima vêm de conhecimento do autor e **não foram reabertas**: confira na doc do Boot/Security da sua versão.

## Testes

A doc de testes (✅) oferece post-processors para simular um JWT sem IdP:

```java
mockMvc.perform(get("/pedidos/1")
        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_pedidos:ler"))))
    .andExpect(status().isOk());

mockMvc.perform(get("/pedidos/1").with(jwt()))      // sem authority -> 403
    .andExpect(status().isForbidden());
```

(`jwt()` vem de `SecurityMockMvcRequestPostProcessors`.)

## Armadilhas e "faça assim"

| Não faça | Faça |
|---|---|
| Só `authenticated()` e achar que está protegido | Autorização por authority/método + validação de `aud` |
| Desabilitar CSRF em tudo "porque deu erro" | Desabilitar **só** em API stateless com Bearer; manter em apps com sessão/cookie |
| Chave pública colada no `application.yml` | `issuer-uri`/`jwk-set-uri` (rotação sem deploy) |
| Implementar `JwtDecoder` com `Signature` à mão | Usar `NimbusJwtDecoder` |
| `permitAll()` em `/actuator/**` inteiro | Só `health` (e `info` se quiser); proteger o resto ou mover para outra porta |
| Logar o token ou o header `Authorization` | Mascarar; logar `sub`/`jti` |

## Fontes

- ✅ Spring Security Reference: *OAuth 2.0 Resource Server — JWT*, *Opaque Token*, *OAuth2 Client — Authorized Clients*, *X.509 Authentication*, *Testing OAuth 2.0*
- ✅ Spring Boot Reference: *SSL*, *REST Clients* (`RestClientSsl`)
- 📎 RFC 6750 (Bearer Token Usage) · RFC 8725

**[← 08 · Keycloak e RH-SSO](08-keycloak-rhsso.md) · [Sumário](../README.md) · [10 · Segredos, chaves e cofres →](10-segredos-e-cofres.md)**
