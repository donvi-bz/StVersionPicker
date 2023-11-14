package biz.donvi.syncthingversionpicker.files;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocationLister {
    /**
     * The path to the root folder with the {@link biz.donvi.syncthingversionpicker.files.StFile.Location LocalReal}
     * files. This will be used as the basis for all relative paths to local real files.
     */
    private final Path localRealPath;

    /**
     * The path to the root folder with the {@link biz.donvi.syncthingversionpicker.files.StFile.Location LocalVersions}
     * files. This will be used as the basis for all relative paths to local versioned files.
     */
    private final Path localVersionsPath;

    /**
     * Constructor for the {@code LocationLister}. TODO: eventually this will take file list producers.
     * @param localRealPath The <b>absolute</b> path to the root directory of the Syncthing folder.
     * @param localVersionsPath The <b>absolute</b> path to the root directory of the {@code .stversions} folder.
     */
    public LocationLister(Path localRealPath, Path localVersionsPath) {
        this.localRealPath = localRealPath;
        this.localVersionsPath = localVersionsPath;
    }

    /**
     * A record that holds the information that we are interested
     * in getting from files & folders that we look at.
     * @param location The location type of the file (basically, where did we find it).
     * @param name The file's name (raw name, un-modified).
     * @param isDir If this file is a directory or not.
     */
    record FileWithLocation(StFile.Location location, String name, boolean isDir) {
        @Override
        public String toString() {
            return "FileWithLocation{" +
                   "location=" + location +
                   ", name='" + name + '\'' +
                   '}';
        }
    }

    /**
     * Lists all files from all sources for a given path.
     * @param path The <b>relative</b> path that we want to list files for.
     * @return A {@link FileWithLocation} record containing the location, name, and if its a directory.
     */
    List<FileWithLocation> listAllFiles(Path path) {
        var files = new ArrayList<FileWithLocation>();
        files.addAll(listLocalRealFiles(localRealPath.resolve(path)));
        files.addAll(listLocalVersionedFiles(localVersionsPath.resolve(path)));
        return files;
    }

    /**
     * Helper method to exclude Syncthing files.
     * @param name The name of the file to check.
     * @return {@code true} if the file should be kept, {@code false} if the file is one of the Syncthing files.
     */
    private boolean notStPlaceholder(String name) {
        return !(name.equals(StFile.STV) || name.equals(StFile.STF));
    }

    private List<FileWithLocation> listLocalRealFiles(Path path) {
        File dir = localRealPath.resolve(path).toFile();
        File[] files = dir.listFiles();
        if (files == null) return List.of();
        return Arrays.stream(files)
                     .sorted()
                     .filter(s -> notStPlaceholder(s.getName()))
                     .map(file -> new FileWithLocation(
                         StFile.Location.LocalReal,
                         file.getName(),
                         file.isDirectory()))
                     .collect(Collectors.toList());
    }

    private List<FileWithLocation> listLocalVersionedFiles(Path path) {
        File dir = localVersionsPath.resolve(path).toFile();
        File[] files = dir.listFiles();
        if (files == null) return List.of();
        return Arrays.stream(files)
                     .sorted()
                     .map(file -> new FileWithLocation(
                         StFile.Location.LocalVersions,
                         file.getName(),
                         file.isDirectory()))
                     .collect(Collectors.toList());
    }

}
