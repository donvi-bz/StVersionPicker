package biz.donvi.syncthingversionpicker.remoteaccess;

import biz.donvi.syncthingversionpicker.SyncPickerApp;
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
import java.util.stream.Collectors;

public class RemoteLister implements LocationLister.Lister {
    String SFTPHOST       = "192.168.68.5";
    int    SFTPPORT       = 22;
    String SFTPUSER       = "";
    String SFTPPASS       = "";
    String SFTPWORKINGDIR = "";
    String SFTPPRIVATEKEY = "";

    Session     session     = null;
    Channel     channel     = null;
    ChannelSftp channelSftp = null;

    public RemoteLister() {

        try {
            JSch jsch = new JSch();
            File privateKey = new File(SFTPPRIVATEKEY);
            if (privateKey.exists() && privateKey.isFile())
                jsch.addIdentity(SFTPPRIVATEKEY);
            session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            session.setPassword(SFTPPASS);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(SFTPWORKINGDIR);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
//            if (session != null) session.disconnect();
//            if (channel != null) channel.disconnect();
        }
    }



    private static boolean isValidFolder(LsEntry f) {
        return !(f.getFilename().equals(".") || f.getFilename().equals(".."));
    }


    @Override
    public CompletableFuture<List<LocationLister.FileWithLocation>> listForDir(Path relativeDirectory) {
        return CompletableFuture.supplyAsync(() -> {
            Vector<LsEntry> files = null;
            String dir = Paths.get(SFTPWORKINGDIR).resolve(relativeDirectory).toString().replace("\\", "/");
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
        }, SyncPickerApp.getApplication().pool);
    }
}
