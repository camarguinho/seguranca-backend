import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * Cliente HTTPS mínimo, só com o JDK.
 *
 * Usando o cacerts do JDK:   java Cliente.java https://localhost:8443/ -
 * Truststore próprio:        java Cliente.java https://localhost:8443/ certs/truststore.p12 changeit
 * Com certificado (mTLS):    java Cliente.java https://localhost:8443/ certs/truststore.p12 changeit certs/cliente.p12 changeit
 *
 * Importante: ao passar um truststore, ele SUBSTITUI o cacerts neste contexto.
 */
public class Cliente {

    public static void main(String[] args) throws Exception {
        String url = args[0];
        String truststore = args[1];

        TrustManager[] confianca = null; // null = usar o padrão da JVM (cacerts)
        if (!truststore.equals("-")) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(carregar(Path.of(truststore), args[2].toCharArray()));
            confianca = tmf.getTrustManagers();
        }

        KeyManager[] identidade = null; // null = o cliente não se identifica
        if (args.length > 4) {
            char[] senhaKeystore = args[4].toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(carregar(Path.of(args[3]), senhaKeystore), senhaKeystore);
            identidade = kmf.getKeyManagers();
        }

        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(identidade, confianca, null);

        // O HttpClient do JDK já confere o hostname contra o SAN do certificado.
        HttpClient cliente = HttpClient.newBuilder().sslContext(contexto).build();
        HttpResponse<String> resposta = cliente.send(
                HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("HTTP " + resposta.statusCode() + " -> " + resposta.body().trim());
    }

    private static KeyStore carregar(Path arquivo, char[] senha) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var entrada = Files.newInputStream(arquivo)) {
            ks.load(entrada, senha);
        }
        return ks;
    }
}
