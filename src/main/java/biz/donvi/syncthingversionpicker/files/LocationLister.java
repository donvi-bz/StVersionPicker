package biz.donvi.syncthingversionpicker.files;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static biz.donvi.syncthingversionpicker.files.StFile.*;

public class LocationLister {
    public static final Lister DEFAULT_LISTER = ignored -> CompletableFuture.completedFuture(List.of());

    private Lister localRealLister      = DEFAULT_LISTER;
    private Lister localVersionsLister  = DEFAULT_LISTER;
    private Lister remoteRealLister     = DEFAULT_LISTER;
    private Lister remoteVersionsLister = DEFAULT_LISTER;

    public LocationLister() {}

    public LocationLister(Path root) {
        this.localRealLister = makeLocalListerForPath(root, Location.LocalReal);
        this.localVersionsLister = makeLocalListerForPath(root.resolve(STV), Location.LocalVersions);
    }

    /**
     * Sets a specific lister.
     *
     * @param location The lister to set.
     * @param lister   The new value for the lister. Set as {@code null} to remove.
     */
    public void setLister(Location location, Lister lister) {
        if (lister == null) lister = DEFAULT_LISTER;
        switch (location) {
            case LocalReal -> localRealLister = lister;
            case RemoteReal -> remoteRealLister = lister;
            case LocalVersions -> localVersionsLister = lister;
            case RemoteVersions -> remoteVersionsLister = lister;
        }
    }

    /** <b>FIXME: OUTDATED</b><br/>
     * Lists all files from all sources for a given path.
     *
     * @param path The <b>relative</b> path that we want to list files for.
     * @return A {@link FileWithLocation} record containing the location, name, and if its a directory.
     */
    CompletableFuture<List<FileWithLocation>> listAllFiles(Path path) {
        @SuppressWarnings("unchecked")
        CompletableFuture<List<FileWithLocation>>[] futures = List.of(
            localRealLister.listForDir(path),
            localVersionsLister.listForDir(path),
            remoteRealLister.listForDir(path),
            remoteVersionsLister.listForDir(path)
        ).toArray(CompletableFuture[]::new);
        return CompletableFuture
            .allOf(futures)
            .thenApplyAsync(x -> {
                var files = new ArrayList<FileWithLocation>();
                for (var f : futures) {
                    files.addAll(f.resultNow());
                }
                return files;
            });
    }

    /**
     * A record that holds the information that we are interested
     * in getting from files & folders that we look at.
     *
     * @param location The location type of the file (basically, where did we find it).
     * @param name     The file's name (raw name, un-modified).
     * @param isDir    If this file is a directory or not.
     */
    public record FileWithLocation(Location location, String name, boolean isDir) {
        @Override
        public String toString() {
            return "FileWithLocation{" +
                   "location=" + location +
                   ", name='" + name + '\'' +
                   '}';
        }
    }

    /**<b>FIXME: OUTDATED</b><br/>
     * Functional interface for anything that can list files in a given <b>relative</b> directory.
     * <br/>
     * Each {@link LocationLister} will have four {@link Lister Lister}s, one for each of the locations in
     * the enum {@link Location Location}. For an example of a lister, see
     * {@link #makeLocalListerForPath(Path, Location)}<br/>
     * Note: Captures are useful for saving the {@code root} path and {@code location}.
     */
    @FunctionalInterface
    public interface Lister {
        /**
         * Returns a list of all the files in a given directory.
         *
         * @param relativeDirectory The <b>relative</b> path to the directory to list.
         * @return A list describing all files within the directory.
         */
        CompletableFuture<List<FileWithLocation>> listForDir(Path relativeDirectory);
    }


    /** <b>FIXME: OUTDATED</b><br/>
     * Helper method to exclude Syncthing files.
     *
     * @param name The name of the file to check.
     * @return {@code true} if the file should be kept, {@code false} if the file is one of the Syncthing files.
     */
    private static boolean notStPlaceholder(String name) {
        return !(name.equals(STV) || name.equals(STF));
    }


    public static Lister makeLocalListerForPath(Path root, Location loc) {
        return relPath -> CompletableFuture.supplyAsync(() -> {
            File dir = root.resolve(relPath).toFile();
            File[] files = dir.listFiles();
            if (files == null) return List.of();
            return Arrays.stream(files)
                         .sorted()
                         .filter(s -> notStPlaceholder(s.getName()))
                         .map(file -> new FileWithLocation(
                             loc,
                             file.getName(),
                             file.isDirectory()))
                         .collect(Collectors.toList());
        });
    }
}
