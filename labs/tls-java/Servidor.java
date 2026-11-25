import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * Servidor HTTPS mínimo, só com o JDK.
 *
 * TLS comum:  java Servidor.java certs/servidor.p12 changeit 8443
 * mTLS:       java Servidor.java certs/servidor.p12 changeit 8443 certs/truststore.p12
 *
 * O keystore diz "quem sou eu". Se um truststore for informado, o servidor passa a
 * exigir o certificado do cliente e só aceita quem foi emitido por uma CA desse truststore.
 */
public class Servidor {

    public static void main(String[] args) throws Exception {
        Path caminhoKeystore = Path.of(args[0]);
        char[] senha = args[1].toCharArray();
        int porta = Integer.parseInt(args[2]);
        Path caminhoTruststore = args.length > 3 ? Path.of(args[3]) : null;
        boolean mtls = caminhoTruststore != null;

        // 1) Identidade do servidor: chave privada + cadeia de certificados
        KeyStore keystore = carregar(caminhoKeystore, senha);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, senha);

        // 2) Confiança (só em mTLS): em quem o servidor confia para validar o cliente
        TrustManager[] confianca = null;
        if (mtls) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(carregar(caminhoTruststore, senha));
            confianca = tmf.getTrustManagers();
        }

        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(kmf.getKeyManagers(), confianca, null);

        HttpsServer servidor = HttpsServer.create(new InetSocketAddress("0.0.0.0", porta), 0);
        servidor.setHttpsConfigurator(new HttpsConfigurator(contexto) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters parametros = contexto.getDefaultSSLParameters();
                if (mtls) {
                    parametros.setNeedClientAuth(true); // sem certificado de cliente, sem conversa
                }
                params.setSSLParameters(parametros);
            }
        });

        servidor.createContext("/", troca -> {
            String quem = "anonimo";
            try {
                quem = ((HttpsExchange) troca).getSSLSession().getPeerPrincipal().getName();
            } catch (SSLPeerUnverifiedException semCertificadoDeCliente) {
                // TLS comum: o cliente não se identificou, e está tudo bem
            }
            byte[] corpo = ("Ola, " + quem + "\n").getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, corpo.length);
            try (var saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });

        servidor.start();
        System.out.println("Servidor HTTPS na porta " + porta + (mtls ? " (mTLS)" : " (TLS comum)"));
    }

    private static KeyStore carregar(Path arquivo, char[] senha) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var entrada = Files.newInputStream(arquivo)) {
            ks.load(entrada, senha);
        }
        return ks;
    }
}
