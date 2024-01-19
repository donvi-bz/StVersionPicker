package biz.donvi.syncthingversionpicker.files;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DirectoryLister {

    /**
     * Returns a list of all the files in a given directory.
     *
     * @param relativeDirectory The path to the directory to list.
     * @param when              Which location to check (either {@code Current} or {@code Versions}
     * @return A list describing all files within the directory.
     */
    CompletableFuture<List<FileWithLocation>> listForDir(Path relativeDirectory, Location.When when);

    /**
     * Returns an input stream from the file. <br/>
     * <b>May complete exceptionally</b>
     *
     * @param relativePath The relative path of the file.
     * @return An input stream from that file.
     */
    CompletableFuture<InputStream> readFile(Path relativePath, Location.When when);

    /**
     * Find the root directory for this lister.
     * @param when Do we want the current or versions directory?
     * @return The root directory for either the current or versions folder.
     */
    Path rootDir(Location.When when);

    /**
     * A record that holds the information that we are interested
     * in getting from files & folders that we look at.
     *
     * @param location The location type of the file (basically, where did we find it).
     * @param name     The file's name (raw name, un-modified).
     * @param isDir    If this file is a directory or not.
     */
    record FileWithLocation(Location location, String name, boolean isDir) {}

    /**
     * A {@link DirectoryLister} that returns empty responses;
     */
    DirectoryLister emptyLister = new DirectoryLister() {

        @Override
        public Path rootDir(Location.When when) {
            return null;
        }

        @Override
        public CompletableFuture<List<FileWithLocation>> listForDir(Path relativeDirectory, Location.When when) {
            return CompletableFuture.completedFuture(List.of());
        }

        @Override
        public CompletableFuture<InputStream> readFile(Path relativePath, Location.When when) {
            CompletableFuture<InputStream> future = new CompletableFuture<>();
            future.completeExceptionally(new FileNotFoundException("The empty lister can never file a file."));
            return future;
        }
    };
}
