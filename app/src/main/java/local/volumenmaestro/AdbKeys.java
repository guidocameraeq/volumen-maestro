package local.volumenmaestro;

import android.content.Context;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

/** Par de claves RSA propio de la app, guardado en su carpeta privada. */
public final class AdbKeys {
    private AdbKeys() {
    }

    public static KeyPair load(Context context) throws Exception {
        File priv = new File(context.getFilesDir(), "adbkey");
        File pub = new File(context.getFilesDir(), "adbkey.pub");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        if (priv.exists() && pub.exists()) {
            return new KeyPair(
                    kf.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(pub.toPath()))),
                    kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(priv.toPath()))));
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(new RSAKeyGenParameterSpec(2048, BigInteger.valueOf(65537)));
        KeyPair keys = gen.generateKeyPair();
        Files.write(priv.toPath(), keys.getPrivate().getEncoded());
        Files.write(pub.toPath(), keys.getPublic().getEncoded());
        return keys;
    }
}
