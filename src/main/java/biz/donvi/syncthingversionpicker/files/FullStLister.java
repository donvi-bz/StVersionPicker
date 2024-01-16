package biz.donvi.syncthingversionpicker.files;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link FullStLister} acts as the single source where all new file information comes from. This class acts as a
 * way to combine the {@link LocalDirectoryLister} and whichever class ends up implementing {@link DirectoryLister}
 * to act as the {@code RemoteDirectoryLister}.
 */
public class FullStLister {

    private final DirectoryLister localDirectoryLister;
    private final DirectoryLister remoteDirectoryLister;

    public FullStLister(DirectoryLister localDirectoryLister, DirectoryLister remoteDirectoryLister) {
        this.localDirectoryLister = localDirectoryLister;
        this.remoteDirectoryLister = remoteDirectoryLister;
    }

    /**
     * Lists all files from all sources for a given path.
     *
     * @param path The <b>relative</b> path that we want to list files for.
     * @return A {@link DirectoryLister.FileWithLocation} record containing the location, name, and if its a directory.
     */
    CompletableFuture<List<DirectoryLister.FileWithLocation>> listAllFiles(Path path) {
        @SuppressWarnings("unchecked")
        CompletableFuture<List<DirectoryLister.FileWithLocation>>[] futures = List.of(
            localDirectoryLister.listForRealDir(path),
            localDirectoryLister.listForRemoteDir(path),
            remoteDirectoryLister.listForRealDir(path),
            remoteDirectoryLister.listForRemoteDir(path)
        ).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApplyAsync(x -> {
            var files = new ArrayList<DirectoryLister.FileWithLocation>();
            for (var f : futures)
                files.addAll(f.resultNow());
            return files;
        });
    }
}
