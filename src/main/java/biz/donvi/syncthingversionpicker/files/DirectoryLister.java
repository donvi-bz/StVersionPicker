package biz.donvi.syncthingversionpicker.files;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DirectoryLister {

    /**
     * Returns a list of all the files in a given directory. <br/>
     * This method always list files in the <b>real</b> folder.
     *
     * @param relativeDirectory The <b>relative</b> path to the directory to list.
     * @return A list describing all files within the directory.
     */
    CompletableFuture<List<FileWithLocation>> listForRealDir(Path relativeDirectory);

    /**
     * Returns a list of all the files in a given directory. <br/>
     * This method always list files in the <b>.stversions</b> folder.
     *
     * @param relativeDirectory The <b>relative</b> path to the directory to list. <br/>
     *                          Make sure <b>not</b> to include the <c>.stversions</c> file in this path when calling.
     * @return A list describing all files within the directory.
     */
    CompletableFuture<List<FileWithLocation>> listForRemoteDir(Path relativeDirectory);

    /**
     * A record that holds the information that we are interested
     * in getting from files & folders that we look at.
     *
     * @param location The location type of the file (basically, where did we find it).
     * @param name     The file's name (raw name, un-modified).
     * @param isDir    If this file is a directory or not.
     */
    record FileWithLocation(StFile.Location location, String name, boolean isDir) {}

    /**
     * A functional interface that represents either of the two function {@link #listForRealDir(Path)} and
     * {@link #listForRemoteDir(Path)}. There is no need to implement this, but it may come in handy for
     * keeping duplicate logic down.
     */
    @FunctionalInterface
    interface IndividualLister {
        CompletableFuture<List<FileWithLocation>> listForDir(Path dir);
    }

    /**
     * A {@link DirectoryLister} that always returns pre-computed empty list.
     */
    DirectoryLister emptyLister = new DirectoryLister() {
        static final CompletableFuture<List<FileWithLocation>> empty = CompletableFuture.completedFuture(List.of());

        @Override
        public CompletableFuture<List<FileWithLocation>> listForRealDir(Path relativeDirectory) {return empty;}

        @Override
        public CompletableFuture<List<FileWithLocation>> listForRemoteDir(Path relativeDirectory) {return empty;}
    };
}
