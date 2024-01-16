package biz.donvi.syncthingversionpicker.remoteaccess;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
import biz.donvi.syncthingversionpicker.files.DirectoryLister;
import biz.donvi.syncthingversionpicker.files.StFile;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RemoteLister implements SingleDirLister {
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);

    private final String host;
    private final int    port;
    private final String user;
    private final String pass;
    private final Path   pathToKey;
    private final RlInfo rlInfo = new RlInfo();

    private final StFile.Location location;

    public RemoteLister(
        String user, String host, int port,
        String pass, Path pathToKey, StFile.Location location
    ) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.pathToKey = pathToKey;
        this.location = location;
        RlPair.addNew(this);
    }


    public CompletableFuture<Optional<JSchException>> setupSessionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setupSession();
                return Optional.empty();
            } catch (JSchException e) {
                return Optional.of(e);
            }
        }, pool);
    }

    public CompletableFuture<Optional<Exception>> setupSessionAndChannelAsync(Path rootDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setupSessionAndChannel(rootDir);
                return Optional.empty();
            } catch (JSchException | SftpException e) {
                return Optional.of(e);
            }
        }, pool);
    }

    private void setupSession() throws JSchException {
        rlInfo.closeConnections();
        JSch jsch = new JSch();
        File privateKey = pathToKey.toFile();
        if (privateKey.exists() && privateKey.isFile())
            jsch.addIdentity(pathToKey.toString());
        rlInfo.session = jsch.getSession(user, host, port);
        rlInfo.session.setPassword(pass);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        rlInfo.session.setConfig(config);
        rlInfo.session.connect(5000); // Hard-coded timeout?
    }

    private void setupSessionAndChannel(Path rootDir) throws JSchException, SftpException {
        this.rlInfo.rootDir = rootDir;
        setupSession();
        rlInfo.channel = rlInfo.session.openChannel("sftp");
        rlInfo.channel.connect();
        rlInfo.channelSftp = (ChannelSftp) rlInfo.channel;
        rlInfo.channelSftp.cd(pathAsStr(rootDir));
    }

//    public StFolder getRemoteFolder(String endpoint, String url, String apiKey) throws JSchException {
//        Session session;
//        JSch jsch = new JSch();
//        File privateKey = pathToKey.toFile();
//        if (privateKey.exists() && privateKey.isFile())
//            jsch.addIdentity(pathToKey.toString());
//        session = jsch.getSession(user, host, port);
//        session.setPassword(pass);
//        java.util.Properties config = new java.util.Properties();
//        config.put("StrictHostKeyChecking", "no");
//        session.setConfig(config);
//        session.connect();
//        Channel channel = session.openChannel("direct-tcpip");
//        @SuppressWarnings("UnnecessaryStringEscape")
//        String request = String.format(
//            """
//            GET %s HTTP/1.1
//            X-API-Key: %s
//            User-Agent: Java/19.0.1
//            Host: %s
//            Accept: */*
//            Connection: keep-alive\r\n\r\n
//            """, endpoint, apiKey, url);
//        ((ChannelDirectTCPIP) channel).setHost("127.0.0.1");
//        ((ChannelDirectTCPIP) channel).setPort(8384);
//        channel.connect();
//        try {
//            OutputStream out = channel.getOutputStream();
//            out.write(request.getBytes(StandardCharsets.UTF_8));
//            out.flush();
//            BufferedReader bin = new BufferedReader(new InputStreamReader(channel.getInputStream()));
//            System.out.println("================");
//            System.out.println(request);
//            System.out.println("----------------");
//            for (String line; (line = bin.readLine()) != null; ) {
//                System.out.println(line);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("================");
//
//        return null;
//    }

    private void ensureConnection() {
        if (!rlInfo.validateConnections()) {
            try {
                setupSessionAndChannel(rlInfo.rootDir);
            } catch (JSchException | SftpException e) {
                System.err.println("Failed to ensure connection was up.");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public CompletableFuture<List<DirectoryLister.FileWithLocation>> listForDir(Path relativeDirectory) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnection();
            Vector<LsEntry> files;
            String dir = pathAsStr(rlInfo.rootDir.resolve(relativeDirectory));
            try {
                files = rlInfo.channelSftp.ls(dir);
            } catch (SftpException e) {
                if (!relativeDirectory.toString().isEmpty() && e.id == 2)
                    return List.of();
                System.out.println(dir);
                e.printStackTrace();
                return List.of();
            }
            return files
                .stream()
                .filter(RemoteLister::isValidFolder)
                .map(file -> new DirectoryLister.FileWithLocation(
                    location,
                    file.getFilename(),
                    file.getAttrs().isDir()))
                .collect(Collectors.toList());
        }, pool);
    }

    private static String pathAsStr(Path p) {
        return p.toString().replace("\\", "/");
    }

    private static boolean isValidFolder(LsEntry f) {
        return !(
            f.getFilename().equals(".") ||
            f.getFilename().equals("..") ||
            f.getFilename().equals(".stversions") ||
            f.getFilename().equals(".stfolder"));
    }

    private record RlPair(WeakReference<RemoteLister> listenerRef, RlInfo info) {
        private static final ArrayList<RlPair> rls = new ArrayList<>();

        static {
            SyncPickerApp.registerShutdownOperation(RlPair::shutdown);
        }

        private static void addNew(RemoteLister lister) {
            RlPair rl = new RlPair(new WeakReference<>(lister), lister.rlInfo);
            pool.submit(() -> {
                rls.add(rl);
                Iterator<RlPair> iterator = rls.iterator();
                while (iterator.hasNext()) {
                    RlPair x = iterator.next();
                    if (x.listenerRef.get() == null) {
                        x.info.closeConnections();
                        iterator.remove();
                    }
                }
            });
        }

        public static void shutdown() {
            CompletableFuture.runAsync(() -> {
                for (RlPair rl : rls)
                    rl.info.closeConnections();
                rls.clear();
                pool.shutdown();
            }, pool);
        }
    }

    private static class RlInfo {
        private Path        rootDir     = null;
        private Session     session     = null;
        private Channel     channel     = null;
        private ChannelSftp channelSftp = null;


        private void closeConnections() {
            if (session != null)
                session.disconnect();
            if (channel != null)
                channel.disconnect();
            session = null;
            channel = null;
        }

        private boolean validateConnections() {
            return session != null && session.isConnected() &&
                   channel != null && channel.isConnected();
        }
    }

    public RemoteLister duplicate(StFile.Location location) {
        return new RemoteLister(user, host, port, pass, pathToKey, location);
    }
}
