package com.neu.riketiku.ai.vision;

import java.util.List;

public record AiVisionRequest(long questionId, List<Image> images, String purpose) {
    public AiVisionRequest {
        images = List.copyOf(images == null ? List.of() : images);
        if (questionId <= 0 || purpose == null || purpose.isBlank()) throw new IllegalArgumentException("Invalid vision request");
        if (images.isEmpty() || images.size() > 2) throw new IllegalArgumentException("Vision request requires one or two images");
        long total=images.stream().mapToLong(image->image.bytes().length).sum();
        if(total>6L*1024*1024)throw new IllegalArgumentException("Vision request exceeds total image limit");
    }

    public record Image(String hash, String mime, byte[] bytes) {
        public Image {
            bytes = bytes == null ? new byte[0] : bytes.clone();
            if(hash==null||hash.isBlank()||!("image/png".equals(mime)||"image/jpeg".equals(mime))
                    ||bytes.length==0||bytes.length>3L*1024*1024)throw new IllegalArgumentException("Invalid vision image");
        }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
}
