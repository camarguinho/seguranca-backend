# 01 · Criptografia básica: as quatro ferramentas

**[← 00 · Introdução: a história do cartório](00-introducao.md) · [Sumário](../README.md) · [02 · Certificados e PKI →](02-certificados-e-pki.md)**

> **Analogia:** o cartório usa quatro coisas. **Lacre** (hash: detecta violação), **senha combinada** entre duas pessoas (HMAC: lacre que só quem sabe o segredo consegue refazer), **cofre com chave única** (AES: esconde conteúdo) e **firma de próprio punho** (assinatura: só você faz, qualquer um confere).

## Mapa mental

```
                    ┌──────────────── sem chave ────────────────┐
  HASH (SHA-256)    │ "impressão digital": mesma entrada → mesmo │
                    │ resultado; 1 bit muda → tudo muda          │
                    └────────────────────────────────────────────┘

  SIMÉTRICA         ┌── 1 chave compartilhada ──┐
   HMAC             │ integridade + autoria     │  quem confere TAMBÉM consegue forjar
   AES-GCM          │ sigilo + integridade      │

  ASSIMÉTRICA       ┌── par de chaves (privada + pública) ──┐
   Assinatura       │ privada assina, pública confere       │  só o dono assina
   (RSA, ECDSA)     │ → base de certificados e JWT RS256    │  qualquer um confere
```

### Por que isso importa para certificados e tokens

- Um **certificado** é um documento + a **assinatura** de uma CA sobre ele.
- Um **JWT RS256** é um texto + a **assinatura** do emissor sobre ele.
- Um **TLS** usa assimétrica para provar identidade e negociar uma chave, e depois **simétrica (AEAD)** para o tráfego.

## Par de chaves

- **Chave privada:** fica com o dono. Nunca viaja, nunca vai ao Git, nunca vai em log.
- **Chave pública:** pode ser distribuída livremente.
- O que uma chave privada assina, a pública correspondente confere. Descobrir a privada a partir da pública deve ser inviável.

## Código: as quatro ferramentas em Java puro

Arquivo: [`labs/cripto-java/Cripto.java`](../labs/cripto-java/Cripto.java). Executar com `java Cripto.java`.

```java
// 1) Hash
byte[] hash = MessageDigest.getInstance("SHA-256").digest(mensagem);

// 2) HMAC (e comparação em tempo constante)
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(segredoBytes, "HmacSHA256"));
byte[] etiqueta = mac.doFinal(mensagem);
boolean confere = MessageDigest.isEqual(etiqueta, mac.doFinal(mensagem)); // nunca equals()

// 3) AES-GCM: IV de 12 bytes NOVO a cada mensagem; etiqueta de 128 bits
Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
c.init(Cipher.ENCRYPT_MODE, chaveAes, new GCMParameterSpec(128, iv));
c.updateAAD(cabecalho);              // autenticado, não cifrado
byte[] cifrado = c.doFinal(mensagem);

// 4) Assinatura RSA
Signature s = Signature.getInstance("SHA256withRSA");
s.initSign(par.getPrivate());
s.update(mensagem);
byte[] assinatura = s.sign();
```

Saída real da execução:

```text
1. SHA-256        : 73a4d4b7...0c327a
   mensagem alterada: 87a54040...cad012          ← uma letra mudou, hash totalmente diferente
2. HMAC-SHA256    : fe6bae5e...78eb7f  confere=true
3. AES-256-GCM    : 36 bytes (mensagem 20 + etiqueta 16)
   decifrado      : pagamento:100.00:BRL
   adulterado     : RECUSADO (AEADBadTagException)   ← 1 bit trocado e o GCM recusa
4. Assinatura RSA : 256 bytes, original confere=true
   mensagem alterada confere=false
```

## Comandos equivalentes no terminal

```bash
# Hash
echo -n "pagamento:100.00:BRL" | sha256sum
echo -n "pagamento:100.00:BRL" | openssl dgst -sha256

# HMAC
echo -n "pagamento:100.00:BRL" | openssl dgst -sha256 -hmac "segredo"

# Par de chaves RSA e assinatura
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out priv.pem
openssl pkey -in priv.pem -pubout -out pub.pem
echo -n "mensagem" > msg.txt
openssl dgst -sha256 -sign priv.pem -out msg.sig msg.txt
openssl dgst -sha256 -verify pub.pem -signature msg.sig msg.txt   # Verified OK
```

## Armadilhas e "faça assim"

| Não faça | Faça |
|---|---|
| Comparar HMAC/token com `equals()` ou `==` | `MessageDigest.isEqual(a, b)` (tempo constante) |
| Reusar o mesmo IV com a mesma chave no AES-GCM | IV novo e aleatório (12 bytes) por mensagem, guardado junto ao cifrado |
| Usar `Cipher.getInstance("AES")` (cai em ECB por padrão no provider SUN) | Sempre informar modo e padding: `AES/GCM/NoPadding` |
| Guardar senha de usuário com SHA-256 | Usar um algoritmo próprio para senhas (ex.: `DelegatingPasswordEncoder` do Spring Security, que usa bcrypt por padrão) |
| Inventar o próprio protocolo de cifra | Usar TLS (transporte) e JWE/bibliotecas padrão, se precisar cifrar payload |
| Gerar aleatórios com `java.util.Random` | `java.security.SecureRandom` |

> ⚠️ A linha sobre ECB como padrão de `Cipher.getInstance("AES")` descreve o comportamento do provider padrão da Oracle/OpenJDK; é um clássico, mas confirme na sua versão.

## Fontes

- 📎 NIST SP 800-38D (AES-GCM) · FIPS 180-4 (SHA-2) · RFC 2104 (HMAC) · RFC 8017 (RSA/PKCS#1)
- 📎 JCA Reference Guide (Oracle/OpenJDK) para `Cipher`, `Signature`, `Mac`, `MessageDigest`
- ✅ Execução real: [`labs/cripto-java`](../labs/cripto-java/)

**[← 00 · Introdução: a história do cartório](00-introducao.md) · [Sumário](../README.md) · [02 · Certificados e PKI →](02-certificados-e-pki.md)**
