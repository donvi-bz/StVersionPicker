package biz.donvi.syncthingversionpicker.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static biz.donvi.syncthingversionpicker.files.Location.LocalCurrent;
import static biz.donvi.syncthingversionpicker.files.Location.LocalVersions;
import static biz.donvi.syncthingversionpicker.files.StFile.notStPlaceholder;

/**
 * A specialized {@link DirectoryLister} whose sole purpose is to list files in local directories.
 */
public class LocalDirectoryLister implements DirectoryLister {
    private final Path realRoot;
    private final Path versionsRoot;

    public LocalDirectoryLister(Path realRoot, Path versionsRoot) {
        this.realRoot = realRoot;
        this.versionsRoot = versionsRoot;
    }

    @Override
    public Path rootDir(Location.When when) {
        return when.which(realRoot, versionsRoot);
    }

    @Override
    public CompletableFuture<List<FileWithLocation>> listForDir(Path relativeDirectory, Location.When when) {
        return CompletableFuture.supplyAsync(() -> {
            File dir = rootDir(when).resolve(relativeDirectory).toFile();
            File[] files = dir.listFiles();
            if (files == null) return List.of();
            return Arrays.stream(files)
                         .sorted()
                         .filter(s -> notStPlaceholder(s.getName()))
                         .map(file -> new FileWithLocation(
                             when.which(LocalCurrent, LocalVersions),
                             file.getName(),
                             file.isDirectory()))
                         .toList();
        });
    }

    @Override
    public CompletableFuture<InputStream> readFile(Path relativePath, Location.When when)  {
        var root = when.which(realRoot, versionsRoot);
        CompletableFuture<InputStream> future = new CompletableFuture<>();
        try {
            future.complete(new FileInputStream(root.resolve(relativePath).toFile()));
        } catch (FileNotFoundException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

}
