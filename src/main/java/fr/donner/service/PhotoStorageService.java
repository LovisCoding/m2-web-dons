package fr.donner.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhotoStorageService {
    private static final String UPLOAD_PREFIX = "/uploads/";
    private static final Map<String, String> MIME_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path root;

    public PhotoStorageService(@Value("${donner.upload-dir:./data/uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Stocke une image importée et retourne son chemin public relatif. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier image fourni");
        }
        String contentType = normalizeContentType(file.getContentType());
        String extension = MIME_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Format d’image non pris en charge (JPEG, PNG, WebP ou GIF uniquement)");
        }
        try {
            Files.createDirectories(root);
            String filename = UUID.randomUUID() + extension;
            Path target = resolveSafePath(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return UPLOAD_PREFIX + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d’enregistrer l’image", e);
        }
    }

    /** Charge une image précédemment importée pour la servir en HTTP. */
    public StoredPhoto load(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable");
        }
        Path target = resolveSafePath(filename);
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable");
        }
        try {
            Resource resource = new UrlResource(target.toUri());
            MediaType mediaType = resolveMediaType(target);
            return new StoredPhoto(resource, mediaType);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable", e);
        }
    }

    /** Supprime le fichier disque si l'URL pointe vers un upload local. */
    public void deleteIfLocalUpload(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(UPLOAD_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(UPLOAD_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        try {
            Path target = resolveSafePath(filename);
            Files.deleteIfExists(target);
        } catch (IOException | ResponseStatusException ignored) {
            // Ne bloque pas la suppression ou la mise à jour d'annonce si le fichier a déjà disparu.
        }
    }

    private Path resolveSafePath(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }
        return target;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    private static MediaType resolveMediaType(Path path) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null && !probed.isBlank()) {
                return MediaType.parseMediaType(probed);
            }
        } catch (IOException | IllegalArgumentException ignored) {
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public record StoredPhoto(Resource resource, MediaType mediaType) { }
}
