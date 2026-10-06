package amoussa.sixtakes.Multijoueur.Net;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Découverte automatique des parties sur le réseau local (diffusion UDP).
 */
public final class LanDiscovery {

    public static final int DISCOVERY_PORT = 3001;
    private static final String PREFIX = "6TAKES;";

    private LanDiscovery() {
    }

    /**
     * Une partie trouvée sur le réseau.
     */
    public static class HostInfo {
        public final String name;
        public final String address;
        public final int port;

        HostInfo(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }

        @Override
        public String toString() {
            return "Partie de " + name + " (" + address + ":" + port + ")";
        }
    }

    /**
     * Annonce périodique d'une partie hébergée.
     */
    public static class Beacon {
        private final String name;
        private final int port;
        private final BooleanSupplier active;
        private volatile boolean running;

        public Beacon(String name, int port, BooleanSupplier active) {
            this.name = name.replace(";", " ");
            this.port = port;
            this.active = active;
        }

        public void start() {
            running = true;
            Thread t = new Thread(this::loop, "lan-beacon");
            t.setDaemon(true);
            t.start();
        }

        public void stop() {
            running = false;
        }

        private void loop() {
            byte[] data = (PREFIX + name + ";" + port).getBytes(StandardCharsets.UTF_8);
            try (DatagramSocket s = new DatagramSocket()) {
                s.setBroadcast(true);
                while (running) {
                    if (active.getAsBoolean()) {
                        for (InetAddress target : broadcastAddresses()) {
                            try {
                                s.send(new DatagramPacket(data, data.length, target, DISCOVERY_PORT));
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Annonce LAN interrompue : " + e.getMessage());
            }
        }
    }

    /**
     * Écouter les annonces pendant une durée donnée.
     *
     * @param timeoutMs
     * @return les parties trouvées
     */
    public static List<HostInfo> search(int timeoutMs) {
        Map<String, HostInfo> found = new LinkedHashMap<>();
        try (DatagramSocket s = new DatagramSocket(null)) {
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(DISCOVERY_PORT));
            long end = System.currentTimeMillis() + timeoutMs;
            byte[] buf = new byte[512];
            while (System.currentTimeMillis() < end) {
                s.setSoTimeout((int) Math.max(1, end - System.currentTimeMillis()));
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    s.receive(p);
                } catch (SocketTimeoutException e) {
                    break;
                }
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                if (!msg.startsWith(PREFIX)) {
                    continue;
                }
                String[] parts = msg.split(";");
                if (parts.length < 3) {
                    continue;
                }
                try {
                    String addr = p.getAddress().getHostAddress();
                    int port = Integer.parseInt(parts[2]);
                    found.put(addr + ":" + port, new HostInfo(parts[1], addr, port));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            System.out.println("Recherche LAN impossible : " + e.getMessage());
        }
        return new ArrayList<>(found.values());
    }

    /**
     * Adresse IPv4 locale de cette machine (pour l'afficher aux autres joueurs).
     *
     * @return
     */
    public static String localAddress() {
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        for (NetworkInterface ni : realInterfaces()) {
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) {
                    continue;
                }
                String ip = a.getHostAddress();
                // box internet en priorité, puis réseaux d'entreprise
                int rank = ip.startsWith("192.168.") ? 0 : ip.startsWith("10.") ? 1 : 2;
                if (rank < bestRank) {
                    best = ip;
                    bestRank = rank;
                }
            }
        }
        return best != null ? best : "127.0.0.1";
    }

    /**
     * Interfaces réseau physiques actives (hors Docker, machines virtuelles, VPN...).
     *
     * @return
     */
    private static List<NetworkInterface> realInterfaces() {
        List<NetworkInterface> res = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String n = ni.getName().toLowerCase();
                boolean virtual = n.startsWith("docker") || n.startsWith("br-") || n.startsWith("veth")
                        || n.startsWith("virbr") || n.startsWith("vmnet") || n.startsWith("vboxnet")
                        || n.startsWith("tun") || n.startsWith("tap") || n.startsWith("tailscale")
                        || n.startsWith("zt");
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual() && !virtual) {
                    res.add(ni);
                }
            }
        } catch (SocketException ignored) {
        }
        return res;
    }

    private static List<InetAddress> broadcastAddresses() {
        List<InetAddress> res = new ArrayList<>();
        try {
            res.add(InetAddress.getByName("255.255.255.255"));
            for (NetworkInterface ni : realInterfaces()) {
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getBroadcast() != null) {
                        res.add(ia.getBroadcast());
                    }
                }
            }
        } catch (UnknownHostException ignored) {
        }
        return res;
    }
}
