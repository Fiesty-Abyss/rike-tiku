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
    void storesSelfContainedSvgAndWebpButRejectsActiveSvgContent() throws Exception {
        QuestionAttachmentStorage storage = new QuestionAttachmentStorage(Files.createTempDirectory("attachment-modern").toString());
        byte[] svg="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\"><circle cx=\"32\" cy=\"32\" r=\"20\"/></svg>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] webp=new byte[]{'R','I','F','F',8,0,0,0,'W','E','B','P','V','P','8',' '};
        assertThat(storage.read(storage.store("science.svg",svg).relativePath(),storage.store("science.svg",svg).hash()).mime()).isEqualTo("image/svg+xml");
        assertThat(storage.store("science.webp",webp).mime()).isEqualTo("image/webp");
        assertThatThrownBy(()->storage.store("unsafe.svg","<svg><script>alert(1)</script></svg>".getBytes())).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThatThrownBy(()->storage.store("external.svg","<svg><image href=\"https://example.com/a.png\"/></svg>".getBytes())).isInstanceOf(RenZhengYeWuYiChang.class);
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
