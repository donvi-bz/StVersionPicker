package biz.donvi.syncthingversionpicker.remoteaccess;

import biz.donvi.syncthingversionpicker.files.LocationLister;
import biz.donvi.syncthingversionpicker.files.StFile;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Path   rootDir;
    private final Path   pathToKey;

    private Session     session     = null;
    private Channel     channel     = null;
    private ChannelSftp channelSftp = null;

    public RemoteLister(
        String user, String host, int port,
        String pass, Path rootDir, Path pathToKey
    ) throws JSchException, SftpException {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.rootDir = rootDir;
        this.pathToKey = pathToKey;
        setupConnection();
    }

    private void setupConnection() throws JSchException, SftpException {
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

    private void closeConnections() {
        if (session != null) session.disconnect();
        if (channel != null) channel.disconnect();
    }

    private boolean validateConnections() {
        return session != null && session.isConnected() &&
               channel != null && channel.isConnected();
    }

    private void ensureConnection() {
        if (!validateConnections()) {
            try {
                setupConnection();
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
