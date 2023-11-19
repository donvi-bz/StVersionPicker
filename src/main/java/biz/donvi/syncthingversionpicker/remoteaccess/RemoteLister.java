package biz.donvi.syncthingversionpicker.remoteaccess;

import biz.donvi.syncthingversionpicker.StFolder;
import biz.donvi.syncthingversionpicker.files.LocationLister;
import biz.donvi.syncthingversionpicker.files.StFile;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RemoteLister implements LocationLister.Lister {
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);

    private final String host;
    private final int    port;
    private final String user;
    private final String pass;
    private final Path   pathToKey;

    private Path        rootDir     = null;
    private Session     session     = null;
    private Channel     channel     = null;
    private ChannelSftp channelSftp = null;

    public RemoteLister(
        String user, String host, int port,
        String pass, Path pathToKey
    ) throws JSchException, SftpException {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.pathToKey = pathToKey;
    }

    public void setupConnection(Path rootDir) throws JSchException, SftpException {
        closeConnections();
        this.rootDir = rootDir;
        JSch jsch = new JSch();
        File privateKey = pathToKey.toFile();
        if (privateKey.exists() && privateKey.isFile())
            jsch.addIdentity(pathToKey.toString());
        session = jsch.getSession(user, host, port);
        session.setPassword(pass);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = session.openChannel("sftp");
        channel.connect();
        channelSftp = (ChannelSftp) channel;
        channelSftp.cd(pathAsStr(rootDir));
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


    private void closeConnections() {
        if (session != null) session.disconnect();
        if (channel != null) channel.disconnect();
        session = null;
        channel = null;
    }

    private boolean validateConnections() {
        return session != null && session.isConnected() &&
               channel != null && channel.isConnected();
    }

    private void ensureConnection() {
        if (!validateConnections()) {
            try {
                setupConnection(rootDir);
            } catch (JSchException | SftpException e) {
                System.err.println("Failed to ensure connection was up.");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public CompletableFuture<List<LocationLister.FileWithLocation>> listForDir(Path relativeDirectory) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnection();
            Vector<LsEntry> files;
            String dir = pathAsStr(rootDir.resolve(relativeDirectory));
            try {
                files = channelSftp.ls(dir);
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
                .map(file -> new LocationLister.FileWithLocation(
                    StFile.Location.RemoteVersions,
                    file.getFilename(),
                    file.getAttrs().isDir()))
                .collect(Collectors.toList());
        }, pool);
    }

    private static String pathAsStr(Path p) {
        return p.toString().replace("\\", "/");
    }

    private static boolean isValidFolder(LsEntry f) {
        return !(f.getFilename().equals(".") || f.getFilename().equals(".."));
    }
}
