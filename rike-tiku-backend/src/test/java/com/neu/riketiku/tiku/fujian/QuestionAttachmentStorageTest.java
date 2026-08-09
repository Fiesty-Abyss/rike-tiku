package com.neu.riketiku.tiku.fujian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class QuestionAttachmentStorageTest {
    @Test
    void storesAndReadsPngAndJpeg() throws Exception {
        QuestionAttachmentStorage storage = new QuestionAttachmentStorage(Files.createTempDirectory("attachment-test").toString());
        for (String extension : new String[]{"png", "jpg"}) {
            var saved = storage.store("a." + extension, image(extension));
            assertThat(saved.hash()).hasSize(64);
            assertThat(storage.read(saved.relativePath(), saved.hash()).mime()).startsWith("image/");
        }
    }

    @Test
    void rejectsTraversalMissingDisguisedAndHashMismatch() throws Exception {
        QuestionAttachmentStorage storage = new QuestionAttachmentStorage(Files.createTempDirectory("attachment-test").toString());
        var saved = storage.store("a.png", image("png"));
        assertThatThrownBy(() -> storage.read("../a.png", saved.hash())).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(() -> storage.read("images/no.png", saved.hash())).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(() -> storage.read(null, saved.hash())).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(() -> storage.store("fake.png", "not an image".getBytes())).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(() -> storage.store("fake.jpg", image("png"))).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(() -> storage.read(saved.relativePath(), "0".repeat(64))).isInstanceOf(RenZhengYeWuYiChang.class);
    }

    @Test
    void rejectsOversizedAndNonRegularSources() throws Exception {
        QuestionAttachmentStorage storage = new QuestionAttachmentStorage(Files.createTempDirectory("attachment-test").toString());
        assertThatThrownBy(() -> storage.store("large.png", new byte[3 * 1024 * 1024 + 1]))
                .isInstanceOf(RenZhengYeWuYiChang.class);
        Path directory = Files.createTempDirectory("attachment-source");
        assertThatThrownBy(() -> storage.store(directory, null)).isInstanceOf(RenZhengYeWuYiChang.class);
    }

    @Test
    void rejectsSymbolicLinkDirectoryEscapesWhenSupported() throws Exception {
        Path storageRoot = Files.createTempDirectory("attachment-symlink-root");
        Path outside = Files.createTempDirectory("attachment-symlink-outside");
        Path imagesLink = storageRoot.resolve("images");
        createSymbolicLinkOrSkip(imagesLink, outside);
        QuestionAttachmentStorage storage = new QuestionAttachmentStorage(storageRoot.toString());
        assertThatThrownBy(() -> storage.store("escape.png", image("png")))
                .isInstanceOf(RenZhengYeWuYiChang.class);

        Path linkedParent = storageRoot.resolve("linked-parent");
        Path nestedOutside = Files.createTempDirectory("attachment-nested-outside");
        createSymbolicLinkOrSkip(linkedParent, nestedOutside);
        QuestionAttachmentStorage nestedStorage = new QuestionAttachmentStorage(linkedParent.resolve("storage").toString());
        assertThatThrownBy(() -> nestedStorage.store("escape.png", image("png")))
                .isInstanceOf(RenZhengYeWuYiChang.class);
    }

    private void createSymbolicLinkOrSkip(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | SecurityException | java.io.IOException exception) {
            Assumptions.abort("当前环境无创建符号链接权限，明确跳过符号链接专项");
        }
    }

    private byte[] image(String type) throws Exception { try(ByteArrayOutputStream out=new ByteArrayOutputStream()){ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),type,out);return out.toByteArray();} }
}
