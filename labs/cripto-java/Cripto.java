import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.HexFormat;

/**
 * As quatro ferramentas da criptografia aplicada, só com o JDK.
 * Executar:  java Cripto.java
 */
public class Cripto {

    public static void main(String[] args) throws Exception {
        byte[] mensagem = "pagamento:100.00:BRL".getBytes(StandardCharsets.UTF_8);

        // 1) Hash: a "impressão digital" da mensagem. Qualquer alteração muda tudo.
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(mensagem);
        System.out.println("1. SHA-256        : " + HexFormat.of().formatHex(hash));
        byte[] alterada = "pagamento:900.00:BRL".getBytes(StandardCharsets.UTF_8);
        System.out.println("   mensagem alterada: " + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(alterada)));

        // 2) HMAC: hash com segredo compartilhado (integridade + autenticidade entre quem tem o segredo)
        SecretKey segredo = new SecretKeySpec(
                "segredo-compartilhado-so-para-o-lab".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(segredo);
        byte[] etiqueta = mac.doFinal(mensagem);
        // Compare etiquetas em tempo constante, nunca com equals() ou ==.
        boolean confere = MessageDigest.isEqual(etiqueta, mac.doFinal(mensagem));
        System.out.println("2. HMAC-SHA256    : " + HexFormat.of().formatHex(etiqueta) + "  confere=" + confere);

        // 3) AES-GCM: sigilo + integridade. O IV (12 bytes) é novo a cada mensagem e viaja junto.
        KeyGenerator gerador = KeyGenerator.getInstance("AES");
        gerador.init(256);
        SecretKey chaveAes = gerador.generateKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] cabecalho = "pedido-42".getBytes(StandardCharsets.UTF_8); // autenticado, mas não cifrado (AAD)

        Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
        cifrador.init(Cipher.ENCRYPT_MODE, chaveAes, new GCMParameterSpec(128, iv));
        cifrador.updateAAD(cabecalho);
        byte[] cifrado = cifrador.doFinal(mensagem);
        System.out.println("3. AES-256-GCM    : " + cifrado.length + " bytes (mensagem " + mensagem.length + " + etiqueta 16)");

        Cipher decifrador = Cipher.getInstance("AES/GCM/NoPadding");
        decifrador.init(Cipher.DECRYPT_MODE, chaveAes, new GCMParameterSpec(128, iv));
        decifrador.updateAAD(cabecalho);
        System.out.println("   decifrado      : " + new String(decifrador.doFinal(cifrado), StandardCharsets.UTF_8));

        cifrado[0] ^= 1; // um único bit adulterado
        try {
            Cipher adulterado = Cipher.getInstance("AES/GCM/NoPadding");
            adulterado.init(Cipher.DECRYPT_MODE, chaveAes, new GCMParameterSpec(128, iv));
            adulterado.updateAAD(cabecalho);
            adulterado.doFinal(cifrado);
        } catch (AEADBadTagException e) {
            System.out.println("   adulterado     : RECUSADO (" + e.getClass().getSimpleName() + ")");
        }

        // 4) Assinatura: chave privada assina, chave pública verifica. É a base de certificados e JWT.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair par = kpg.generateKeyPair();

        Signature assinador = Signature.getInstance("SHA256withRSA");
        assinador.initSign(par.getPrivate());
        assinador.update(mensagem);
        byte[] assinatura = assinador.sign();

        Signature verificador = Signature.getInstance("SHA256withRSA");
        verificador.initVerify(par.getPublic());
        verificador.update(mensagem);
        System.out.println("4. Assinatura RSA : " + assinatura.length + " bytes, original confere=" + verificador.verify(assinatura));

        verificador.initVerify(par.getPublic());
        verificador.update(alterada);
        System.out.println("   mensagem alterada confere=" + verificador.verify(assinatura));
    }
}
