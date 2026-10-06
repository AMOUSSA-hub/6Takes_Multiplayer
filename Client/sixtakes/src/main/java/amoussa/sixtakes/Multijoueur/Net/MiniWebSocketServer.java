package amoussa.sixtakes.Multijoueur.Net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serveur WebSocket minimal (RFC 6455, messages texte uniquement),
 * utilisé pour héberger une partie en réseau local.
 */
public class MiniWebSocketServer {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * Callbacks appelés par le serveur (depuis les threads réseau).
     */
    public interface Handler {
        void onOpen(Connection c);

        void onMessage(Connection c, String message);

        void onClose(Connection c);
    }

    /**
     * Une connexion cliente.
     */
    public static class Connection {
        private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

        private final Socket socket;
        private final OutputStream out;
        private final String id;
        private volatile boolean open = true;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new BufferedOutputStream(socket.getOutputStream());
            this.id = "p" + NEXT_ID.getAndIncrement();
        }

        public String getId() {
            return id;
        }

        /**
         * Envoyer un message texte au client.
         *
         * @param text
         */
        public synchronized void send(String text) {
            if (!open) {
                return;
            }
            try {
                writeFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                close();
            }
        }

        private synchronized void writeFrame(int opcode, byte[] payload) throws IOException {
            out.write(0x80 | opcode);
            if (payload.length < 126) {
                out.write(payload.length);
            } else if (payload.length < 65536) {
                out.write(126);
                out.write(payload.length >>> 8);
                out.write(payload.length);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((int) ((long) payload.length >>> (8 * i)));
                }
            }
            out.write(payload);
            out.flush();
        }

        /**
         * Fermer la connexion.
         */
        public void close() {
            open = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private final int port;
    private final Handler handler;
    private ServerSocket serverSocket;

    public MiniWebSocketServer(int port, Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    /**
     * Démarrer l'écoute (non bloquant).
     *
     * @throws IOException si le port est déjà utilisé
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        Thread t = new Thread(this::acceptLoop, "ws-accept");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Arrêter le serveur.
     */
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public int getPort() {
        return port;
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                Thread t = new Thread(() -> handle(s), "ws-client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handle(Socket s) {
        Connection c = null;
        try {
            InputStream in = new BufferedInputStream(s.getInputStream());
            if (!handshake(in, s.getOutputStream())) {
                s.close();
                return;
            }
            c = new Connection(s);
            handler.onOpen(c);
            readFrames(in, c);
        } catch (SocketException | EOFException e) {
            // connexion fermée par le client
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                handler.onClose(c);
            } else {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean handshake(InputStream in, OutputStream rawOut) throws Exception {
        String key = null;
        String line;
        while (!(line = readLine(in)).isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0 && line.substring(0, idx).trim().equalsIgnoreCase("Sec-WebSocket-Key")) {
                key = line.substring(idx + 1).trim();
            }
        }
        if (key == null) {
            rawOut.write("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            rawOut.flush();
            return false;
        }
        byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + GUID).getBytes(StandardCharsets.US_ASCII));
        String accept = Base64.getEncoder().encodeToString(sha1);
        String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        rawOut.write(resp.getBytes(StandardCharsets.US_ASCII));
        rawOut.flush();
        return true;
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                sb.append((char) b);
            }
            if (sb.length() > 8192) {
                throw new IOException("En-tête trop long");
            }
        }
        if (b == -1 && sb.length() == 0) {
            throw new EOFException();
        }
        return sb.toString();
    }

    private void readFrames(InputStream in, Connection c) throws IOException {
        DataInputStream din = new DataInputStream(in);
        ByteArrayOutputStream fragments = new ByteArrayOutputStream();
        while (true) {
            int b0 = din.readUnsignedByte();
            int b1 = din.readUnsignedByte();
            boolean fin = (b0 & 0x80) != 0;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;
            if (len == 126) {
                len = din.readUnsignedShort();
            } else if (len == 127) {
                len = din.readLong();
            }
            if (len > 1_000_000) {
                throw new IOException("Message trop volumineux");
            }
            byte[] mask = new byte[4];
            if (masked) {
                din.readFully(mask);
            }
            byte[] payload = new byte[(int) len];
            din.readFully(payload);
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i % 4];
                }
            }

            switch (opcode) {
                case 0x0: // continuation
                case 0x1: // texte
                    fragments.write(payload);
                    if (fin) {
                        handler.onMessage(c, fragments.toString(StandardCharsets.UTF_8));
                        fragments.reset();
                    }
                    break;
                case 0x8: // fermeture
                    try {
                        c.writeFrame(0x8, new byte[0]);
                    } catch (IOException ignored) {
                    }
                    return;
                case 0x9: // ping
                    c.writeFrame(0xA, payload);
                    break;
                default:
                    // pong / binaire : ignorés
                    break;
            }
        }
    }
}
