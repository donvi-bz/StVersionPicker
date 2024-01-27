package biz.donvi.syncthingversionpicker.files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link FullStLister} acts as the single source where all new file information comes from. This class acts as a
 * way to combine the {@link LocalDirectoryLister} and whichever class ends up implementing {@link DirectoryLister}
 * to act as the {@code RemoteDirectoryLister}.
 */
public class FullStLister {
    private static final Logger logger = LogManager.getLogger(FullStLister.class);

    private final DirectoryLister localDirectoryLister;
    private final DirectoryLister remoteDirectoryLister;

    private final Map<Class<? extends FileService>, FileService> serviceMap = new HashMap<>();

    public FullStLister(DirectoryLister localDirectoryLister, DirectoryLister remoteDirectoryLister) {
        this.localDirectoryLister = localDirectoryLister;
        this.remoteDirectoryLister = remoteDirectoryLister;
    }

    public Path rootDir(Location location) {
        return directoryLister(location.where).rootDir(location.when);
    }

    private DirectoryLister directoryLister(Location.Where where) {
        return where.which(localDirectoryLister, remoteDirectoryLister);
    }

    /**
     * Lists all files from all sources for a given path.
     *
     * @param path The <b>relative</b> path that we want to list files for.
     * @return A {@link DirectoryLister.FileWithLocation} record containing the location, name, and if its a directory.
     */
    CompletableFuture<List<DirectoryLister.FileWithLocation>> listAllFiles(Path path) {
        @SuppressWarnings("unchecked")
        CompletableFuture<List<DirectoryLister.FileWithLocation>>[] futures = Arrays
            .stream(Location.values())
            .map(loc -> directoryLister(loc.where).listForDir(path, loc.when))
            .toArray(CompletableFuture[]::new);
//        @SuppressWarnings("unchecked")
//        CompletableFuture<List<DirectoryLister.FileWithLocation>>[] futures = List.of(
//            localDirectoryLister.listForDir(path, Location.When.Current),
//            localDirectoryLister.listForDir(path, Location.When.Version),
//            remoteDirectoryLister.listForDir(path, Location.When.Current),
//            remoteDirectoryLister.listForDir(path, Location.When.Version)
//        ).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenApplyAsync(x -> {
            var files = new ArrayList<DirectoryLister.FileWithLocation>();
            for (var f : futures)
                files.addAll(f.resultNow());
            return files;
        });
    }

    public CompletableFuture<InputStream> readFile(Path absolutePath, Location location) {
        return directoryLister(location.where).readFile(absolutePath, location.when);
    }

    /* **************************************************************
     MARK: - FileService
     ****************************************************************/

    public interface FileService {}

    public static class FileServiceNotFoundException extends RuntimeException {
        public <T extends FileService> FileServiceNotFoundException(Class<T> clazz) {
            super("FileService for class `%s` not found.".formatted(clazz));
        }
    }

    public <T extends FileService> void setService(Class<T> clazz, T instance) {
        logger.info("New FileService for class `{}` has been added.", clazz);
        serviceMap.put(clazz, instance);
    }

    @SuppressWarnings("unchecked")
    public <T extends FileService> T getService(Class<T> clazz) {
        FileService service = serviceMap.get(clazz);
        if (service != null)
            logger.trace("FileService for class `{}` has been retrieved.", clazz);
        else {
            logger.error("FileService for class `{}` was requested but could not be found. Escalating.", clazz);
            throw new FileServiceNotFoundException(clazz);
        }
        return (T) service;
    }

}
