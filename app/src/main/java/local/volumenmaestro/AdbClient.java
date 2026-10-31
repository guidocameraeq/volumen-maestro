package local.volumenmaestro;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Cliente mínimo del protocolo ADB por TCP. Se conecta al adbd de la propia TV
 * (127.0.0.1:5555) y corre comandos de shell con permisos de "shell".
 */
public final class AdbClient implements Closeable {
    private static final int CNXN = 0x4e584e43;
    private static final int AUTH = 0x48545541;
    private static final int OPEN = 0x4e45504f;
    private static final int OKAY = 0x59414b4f;
    private static final int CLSE = 0x45534c43;
    private static final int WRTE = 0x45545257;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private static final int VERSION = 0x01000000;
    private static final int MAX_DATA = 4096;

    // DigestInfo de SHA-1: adbd firma el token como si ya fuera un hash SHA-1.
    private static final byte[] SHA1_PREFIX = {
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    /** Avisa cuando la TV muestra el cartel de "Permitir depuración". */
    public interface AuthListener {
        void onWaitingForUser();
    }

    private static final class Message {
        int command, arg0, arg1;
        byte[] data;
    }

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private int nextLocalId = 1;

    private AdbClient(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = socket.getOutputStream();
    }

    public static AdbClient connect(String host, int port, KeyPair keys, AuthListener listener)
            throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 3000);
        s.setSoTimeout(5000);
        AdbClient c = new AdbClient(s);
        try {
            c.handshake(keys, listener);
        } catch (IOException | RuntimeException e) {
            c.close();
            throw e;
        }
        s.setSoTimeout(5000);
        return c;
    }

    private void handshake(KeyPair keys, AuthListener listener) throws IOException {
        send(CNXN, VERSION, MAX_DATA, "host::\0".getBytes(StandardCharsets.UTF_8));
        boolean sentSignature = false;
        boolean sentKey = false;
        while (true) {
            Message m = read();
            if (m.command == CNXN) return;
            if (m.command != AUTH || m.arg0 != AUTH_TOKEN) continue;
            if (!sentSignature) {
                send(AUTH, AUTH_SIGNATURE, 0, sign(keys, m.data));
                sentSignature = true;
            } else if (!sentKey) {
                // La TV no conoce la clave: se la mandamos y espera que el usuario acepte.
                send(AUTH, AUTH_RSAPUBLICKEY, 0, publicKeyPayload((RSAPublicKey) keys.getPublic()));
                sentKey = true;
                socket.setSoTimeout(90_000);
                if (listener != null) listener.onWaitingForUser();
            } else {
                throw new IOException("Autorización rechazada");
            }
        }
    }

    /** Corre un comando de shell y devuelve su salida. */
    public synchronized String shell(String command) throws IOException {
        int local = nextLocalId++;
        send(OPEN, local, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int remote = 0;
        while (true) {
            Message m = read();
            if (m.arg1 != local) continue; // restos de un stream anterior
            switch (m.command) {
                case OKAY:
                    remote = m.arg0;
                    break;
                case WRTE:
                    output.write(m.data);
                    send(OKAY, local, m.arg0, new byte[0]);
                    break;
                case CLSE:
                    if (remote != 0) send(CLSE, local, remote, new byte[0]);
                    return output.toString("UTF-8");
                default:
                    break;
            }
        }
    }

    /** Recibe la salida de un comando que no termina (por ejemplo getevent) a medida que llega. */
    public interface StreamListener {
        void onText(String text);
    }

    /**
     * Corre un comando largo y le pasa la salida a [listener] hasta que el comando termina o se
     * corta la conexión. Bloquea: usarlo en un hilo propio y con una conexión dedicada.
     */
    public void stream(String command, StreamListener listener) throws IOException {
        socket.setSoTimeout(0); // puede pasar mucho tiempo sin salida
        int local = nextLocalId++;
        send(OPEN, local, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        int remote = 0;
        while (true) {
            Message m = read();
            if (m.arg1 != local) continue;
            if (m.command == OKAY) {
                remote = m.arg0;
            } else if (m.command == WRTE) {
                send(OKAY, local, m.arg0, new byte[0]);
                listener.onText(new String(m.data, StandardCharsets.UTF_8));
            } else if (m.command == CLSE) {
                if (remote != 0) send(CLSE, local, remote, new byte[0]);
                return;
            }
        }
    }

    /** Reinicia la TV con el servicio "reboot:" de adbd (lo mismo que hace "adb reboot"). */
    public synchronized void reboot() throws IOException {
        send(OPEN, nextLocalId++, 0, "reboot:\0".getBytes(StandardCharsets.UTF_8));
    }

    private void send(int command, int arg0, int arg1, byte[] data) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(24 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        int checksum = 0;
        for (byte d : data) checksum += d & 0xff;
        b.putInt(command).putInt(arg0).putInt(arg1).putInt(data.length)
                .putInt(checksum).putInt(~command).put(data);
        out.write(b.array());
        out.flush();
    }

    private Message read() throws IOException {
        byte[] header = new byte[24];
        in.readFully(header);
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        Message m = new Message();
        m.command = b.getInt();
        m.arg0 = b.getInt();
        m.arg1 = b.getInt();
        int length = b.getInt();
        if (length < 0 || length > 1024 * 1024) throw new IOException("Mensaje ADB inválido");
        m.data = new byte[length];
        in.readFully(m.data);
        return m;
    }

    private static byte[] sign(KeyPair keys, byte[] token) throws IOException {
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, keys.getPrivate());
            c.update(SHA1_PREFIX);
            return c.doFinal(token);
        } catch (Exception e) {
            throw new IOException("No se pudo firmar el token", e);
        }
    }

    /** Clave pública en el formato RSAPublicKey de Android que espera adbd. */
    private static byte[] publicKeyPayload(RSAPublicKey key) {
        final int words = 64; // 2048 bits
        BigInteger r32 = BigInteger.ZERO.setBit(32);
        BigInteger n = key.getModulus();
        BigInteger rr = BigInteger.ZERO.setBit(words * 32).modPow(BigInteger.valueOf(2), n);
        BigInteger n0inv = n.remainder(r32).modInverse(r32);

        ByteBuffer b = ByteBuffer.allocate(4 + 4 + words * 4 * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(words);
        b.putInt(n0inv.negate().intValue());
        BigInteger nn = n;
        for (int i = 0; i < words; i++) {
            BigInteger[] qr = nn.divideAndRemainder(r32);
            nn = qr[0];
            b.putInt(qr[1].intValue());
        }
        for (int i = 0; i < words; i++) {
            BigInteger[] qr = rr.divideAndRemainder(r32);
            rr = qr[0];
            b.putInt(qr[1].intValue());
        }
        b.putInt(key.getPublicExponent().intValue());

        String text = Base64.getEncoder().encodeToString(b.array()) + " volumen-maestro@tv\0";
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
