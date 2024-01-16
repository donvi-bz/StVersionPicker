package biz.donvi.syncthingversionpicker.files;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static biz.donvi.syncthingversionpicker.files.StFile.notStPlaceholder;

/**
 * A specialized {@link DirectoryLister} whose sole purpose is to list files in local directories.
 */
public class LocalDirectoryLister implements DirectoryLister {
    private final Path realRoot;
    private final Path versionsRoot;

    private final IndividualLister realLister;
    private final IndividualLister versionsLister;

    public LocalDirectoryLister(Path realRoot, Path versionsRoot) {
        this.realRoot = realRoot;
        this.versionsRoot = versionsRoot;
        this.realLister = listerBuilder(realRoot, StFile.Location.LocalReal);
        this.versionsLister = listerBuilder(versionsRoot, StFile.Location.LocalVersions);
    }

    @Override
    public CompletableFuture<List<FileWithLocation>> listForRealDir(Path relativeDirectory) {
        return realLister.listForDir(relativeDirectory);
    }

    @Override
    public CompletableFuture<List<FileWithLocation>> listForRemoteDir(Path relativeDirectory) {
        return versionsLister.listForDir(relativeDirectory);
    }

    /**
     * Since the two methods, {@link #listForRealDir(Path)} and {@link #listForRemoteDir(Path)}, are identical
     * logic wise, this static method is used to keep code duplication down. This method takes in a root directory
     * and a location, and will give a {@link biz.donvi.syncthingversionpicker.files.DirectoryLister.IndividualLister
     * IndividualLister} who uses that root path and location for itself. <br/>
     * <p>
     * See {@link DirectoryLister#listForRealDir(Path)} and {@link DirectoryLister#listForRemoteDir(Path)} for more
     * information on that the functions returned by this method will do.
     *
     * @param root     The root path of the folder, adjusted for the version directory if necessary
     * @param location The {@link biz.donvi.syncthingversionpicker.files.StFile.Location Location} that files this
     *                 lister produces are from.
     * @return An {@link biz.donvi.syncthingversionpicker.files.DirectoryLister.IndividualLister IndividualLister}
     * that can be used for one of the two methods a {@link DirectoryLister} must implement.
     */
    private static IndividualLister listerBuilder(Path root, StFile.Location location) {
        return (relativeDir) -> CompletableFuture.supplyAsync(() -> {
            File dir = root.resolve(relativeDir).toFile();
            File[] files = dir.listFiles();
            if (files == null) return List.of();
            return Arrays.stream(files)
                         .sorted()
                         .filter(s -> notStPlaceholder(s.getName()))
                         .map(file -> new FileWithLocation(
                             location,
                             file.getName(),
                             file.isDirectory()))
                         .collect(Collectors.toList());
        });
    }
}
