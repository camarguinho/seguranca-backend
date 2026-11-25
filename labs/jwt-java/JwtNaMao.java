import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JWT "na mão", só com o JDK, para enxergar o que uma biblioteca faz por baixo.
 *
 * ATENÇÃO: é material didático. Em produção use uma biblioteca madura
 * (Nimbus JOSE + JWT, que o Spring Security já usa) e não escreva validação própria.
 *
 * Executar:  java JwtNaMao.java
 */
public class JwtNaMao {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final String ALGORITMO_ACEITO = "RS256"; // vem da CONFIGURAÇÃO, nunca do token

    public static void main(String[] args) throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        KeyPair par = gerador.generateKeyPair();

        long agora = System.currentTimeMillis() / 1000;
        String cabecalho = "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"chave-2026-11\"}";
        String corpo = ("{\"iss\":\"https://idp.exemplo.com/realms/loja\",\"sub\":\"u-123\","
                + "\"aud\":\"api-pedidos\",\"scope\":\"pedidos:ler\",\"iat\":%d,\"exp\":%d}")
                .formatted(agora, agora + 300);

        // 1) Assinar: o emissor usa a chave PRIVADA sobre "cabecalho.corpo"
        String aAssinar = B64.encodeToString(bytes(cabecalho)) + "." + B64.encodeToString(bytes(corpo));
        Signature assinador = Signature.getInstance("SHA256withRSA");
        assinador.initSign(par.getPrivate());
        assinador.update(aAssinar.getBytes(StandardCharsets.US_ASCII));
        String jwt = aAssinar + "." + B64.encodeToString(assinador.sign());

        System.out.println("== 1. O token (três partes separadas por ponto)");
        for (String parte : jwt.split("\\.")) {
            System.out.println("   " + parte.substring(0, Math.min(60, parte.length())) + "...");
        }

        System.out.println("\n== 2. Cabeçalho e corpo são só Base64URL: qualquer um LÊ (não é segredo)");
        String[] partes = jwt.split("\\.");
        System.out.println("   " + new String(DEC.decode(partes[0]), StandardCharsets.UTF_8));
        System.out.println("   " + new String(DEC.decode(partes[1]), StandardCharsets.UTF_8));

        System.out.println("\n== 3. Validar com a chave PÚBLICA (o que o resource server faz)");
        System.out.println("   token íntegro      -> " + validar(jwt, par.getPublic(), agora));

        String adulterado = partes[0] + "."
                + B64.encodeToString(bytes(corpo.replace("pedidos:ler", "pedidos:apagar"))) + "." + partes[2];
        System.out.println("   escopo adulterado  -> " + validar(adulterado, par.getPublic(), agora));

        String semAssinatura = B64.encodeToString(bytes("{\"alg\":\"none\"}")) + "." + partes[1] + ".";
        System.out.println("   alg=none           -> " + validar(semAssinatura, par.getPublic(), agora));

        System.out.println("   token expirado     -> " + validar(jwt, par.getPublic(), agora + 3600));

        System.out.println("\n== 4. A chave pública que o IdP publica no JWKS (formato JWK)");
        System.out.println("   " + jwk((RSAPublicKey) par.getPublic(), "chave-2026-11"));
    }

    /** Devolve "OK" ou o motivo da recusa. A ordem importa: primeiro assinatura, depois claims. */
    static String validar(String jwt, PublicKey chave, long agora) throws Exception {
        String[] p = jwt.split("\\.", -1);
        if (p.length != 3) return "RECUSADO: formato inválido";

        String cabecalho = new String(DEC.decode(p[0]), StandardCharsets.UTF_8);
        String alg = texto(cabecalho, "alg");
        // Regra de ouro (RFC 8725): o algoritmo aceito é decidido pelo servidor, não pelo token.
        if (!ALGORITMO_ACEITO.equals(alg)) return "RECUSADO: algoritmo '" + alg + "' não é aceito";

        Signature verificador = Signature.getInstance("SHA256withRSA");
        verificador.initVerify(chave);
        verificador.update((p[0] + "." + p[1]).getBytes(StandardCharsets.US_ASCII));
        if (!verificador.verify(DEC.decode(p[2]))) return "RECUSADO: assinatura não confere";

        String corpo = new String(DEC.decode(p[1]), StandardCharsets.UTF_8);
        if (!"https://idp.exemplo.com/realms/loja".equals(texto(corpo, "iss"))) return "RECUSADO: issuer inesperado";
        if (!"api-pedidos".equals(texto(corpo, "aud"))) return "RECUSADO: audience inesperada";
        if (numero(corpo, "exp") <= agora) return "RECUSADO: token expirado";
        return "OK (assinatura, iss, aud e exp conferem)";
    }

    /** JWK de chave pública RSA: n e e em Base64URL, sem zeros à esquerda (RFC 7518, seção 6.3). */
    static String jwk(RSAPublicKey k, String kid) {
        return "{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + kid + "\","
                + "\"n\":\"" + B64.encodeToString(semSinal(k.getModulus())) + "\","
                + "\"e\":\"" + B64.encodeToString(semSinal(k.getPublicExponent())) + "\"}";
    }

    private static byte[] semSinal(BigInteger v) {
        byte[] b = v.toByteArray();
        return (b.length > 1 && b[0] == 0) ? java.util.Arrays.copyOfRange(b, 1, b.length) : b;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // Extração por regex apenas para manter o exemplo sem dependências. Não faça isso em produção.
    private static String texto(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static long numero(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }
}
