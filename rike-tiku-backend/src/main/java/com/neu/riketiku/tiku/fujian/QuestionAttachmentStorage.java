package com.neu.riketiku.tiku.fujian;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Stores only verified PNG/JPEG question images under an application-controlled relative path. */
@Service
public class QuestionAttachmentStorage {
    private static final long MAX_BYTES = 3L * 1024 * 1024;
    private final Path root;

    public QuestionAttachmentStorage(@Value("${rike.tiku.attachment.storage-root:./data/question-attachments}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredImage store(Path source, String expectedHash) {
        try {
            if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(source)) fail("ATTACHMENT_FILE_INVALID", "附件文件不可读取", HttpStatus.UNPROCESSABLE_ENTITY);
            return store(source.getFileName().toString(), Files.readAllBytes(source), expectedHash);
        } catch (IOException exception) {
            throw new RenZhengYeWuYiChang("ATTACHMENT_FILE_INVALID", "附件文件无法读取", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public StoredImage store(String originalName, byte[] bytes) {
        return store(originalName, bytes, null);
    }

    private StoredImage store(String originalName, byte[] bytes, String expectedHash) {
        ImageFact fact = validate(originalName, bytes);
        if (expectedHash != null && !expectedHash.equalsIgnoreCase(fact.hash())) fail("ATTACHMENT_HASH_MISMATCH", "附件校验摘要不一致", HttpStatus.UNPROCESSABLE_ENTITY);
        Path relative = Path.of("images", fact.hash().substring(0, 2), fact.hash() + fact.extension());
        Path target = safeTarget(relative);
        try {
            Files.createDirectories(target.getParent());
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) Files.write(target, bytes);
            return new StoredImage(relative.toString().replace('\\', '/'), fact.hash(), fact.mime(), bytes);
        } catch (IOException exception) {
            throw new RenZhengYeWuYiChang("ATTACHMENT_STORE_FAILED", "附件文件无法安全保存", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public StoredImage read(String relativePath, String expectedHash) {
        Path target = safeTarget(relativePath);
        try {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(target)) fail("ATTACHMENT_FILE_MISSING", "附件文件暂不可用", HttpStatus.NOT_FOUND);
            byte[] bytes = Files.readAllBytes(target);
            ImageFact fact = validate(target.getFileName().toString(), bytes);
            if (expectedHash == null || !expectedHash.equalsIgnoreCase(fact.hash())) fail("ATTACHMENT_HASH_MISMATCH", "附件文件校验失败", HttpStatus.CONFLICT);
            return new StoredImage(relativePath.replace('\\', '/'), fact.hash(), fact.mime(), bytes);
        } catch (IOException exception) {
            throw new RenZhengYeWuYiChang("ATTACHMENT_FILE_MISSING", "附件文件暂不可用", HttpStatus.NOT_FOUND);
        }
    }

    public String renderStatus(String relativePath, String expectedHash, String type, String status) {
        if (!"ACTIVE".equals(status) || !"IMAGE".equals(type)) return "UNSUPPORTED";
        try {
            read(relativePath, expectedHash);
            return "AVAILABLE";
        } catch (RenZhengYeWuYiChang exception) {
            return "ATTACHMENT_FILE_MISSING".equals(exception.getCode()) ? "MISSING" : "UNSAFE";
        }
    }

    public void delete(String relativePath) {
        try { Files.deleteIfExists(safeTarget(relativePath)); }
        catch (IOException exception) { throw new RenZhengYeWuYiChang("ATTACHMENT_DELETE_FAILED", "附件文件无法清理", HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    private Path safeTarget(String value) {
        if (value == null || value.isBlank()) fail("ATTACHMENT_PATH_INVALID", "附件路径不合法", HttpStatus.BAD_REQUEST);
        try {
            return safeTarget(Path.of(value));
        } catch (InvalidPathException exception) {
            fail("ATTACHMENT_PATH_INVALID", "附件路径不合法", HttpStatus.BAD_REQUEST);
            return null;
        }
    }

    private Path safeTarget(Path relative) {
        if (relative.isAbsolute() || relative.toString().contains("..")) fail("ATTACHMENT_PATH_INVALID", "附件路径不合法", HttpStatus.BAD_REQUEST);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) fail("ATTACHMENT_PATH_INVALID", "附件路径不合法", HttpStatus.BAD_REQUEST);
        rejectSymbolicLinkComponents(target);
        return target;
    }

    private void rejectSymbolicLinkComponents(Path target) {
        Path filesystemRoot = target.getRoot();
        Path current = filesystemRoot == null ? Path.of("") : filesystemRoot;
        Path components = filesystemRoot == null ? target : filesystemRoot.relativize(target);
        for (Path component : components) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                fail("ATTACHMENT_PATH_INVALID", "附件路径不能经过符号链接", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private ImageFact validate(String name, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) fail("ATTACHMENT_FILE_INVALID", "附件大小不合法", HttpStatus.UNPROCESSABLE_ENTITY);
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String extension = lower.endsWith(".png") ? ".png" : lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? ".jpg" : null;
        if (extension == null) fail("ATTACHMENT_FILE_INVALID", "仅支持 PNG 或 JPEG 图片附件", HttpStatus.UNPROCESSABLE_ENTITY);
        String mime = "png".equals(format(bytes)) ? "image/png" : "jpeg".equals(format(bytes)) ? "image/jpeg" : null;
        if (mime == null || (".png".equals(extension) && !"image/png".equals(mime)) || (".jpg".equals(extension) && !"image/jpeg".equals(mime))) fail("ATTACHMENT_FILE_INVALID", "附件扩展名与实际图片类型不一致", HttpStatus.UNPROCESSABLE_ENTITY);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) fail("ATTACHMENT_FILE_INVALID", "附件不是有效图片", HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException exception) { fail("ATTACHMENT_FILE_INVALID", "附件不是有效图片", HttpStatus.UNPROCESSABLE_ENTITY); }
        return new ImageFact(extension, mime, sha256(bytes));
    }

    private String format(byte[] value) {
        if (value.length >= 8 && value[0] == (byte) 0x89 && value[1] == 0x50 && value[2] == 0x4e && value[3] == 0x47) return "png";
        if (value.length >= 3 && value[0] == (byte) 0xff && value[1] == (byte) 0xd8 && value[2] == (byte) 0xff) return "jpeg";
        return "";
    }

    private String sha256(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (Exception exception) { throw new IllegalStateException("无法计算附件摘要", exception); }
    }

    private void fail(String code, String message, HttpStatus status) { throw new RenZhengYeWuYiChang(code, message, status); }

    public record StoredImage(String relativePath, String hash, String mime, byte[] bytes) { }
    private record ImageFact(String extension, String mime, String hash) { }
}
