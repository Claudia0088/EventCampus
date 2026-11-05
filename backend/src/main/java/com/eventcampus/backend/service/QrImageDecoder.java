package com.eventcampus.backend.service;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Map;

public final class QrImageDecoder {
    private QrImageDecoder() {}
    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image requise (PNG/JPEG, 5 Mo maximum).");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException();
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw new IllegalArgumentException();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096) throw new IllegalArgumentException();
                var image = reader.read(0);
                var source = new RGBLuminanceSource(width, height, image.getRGB(0, 0, width, height, null, 0, width));
                return new com.google.zxing.qrcode.QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source)),
                        Map.of(DecodeHintType.TRY_HARDER, true)).getText();
            } finally { reader.dispose(); }
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun QR code lisible dans cette image PNG/JPEG (4096 pixels maximum).");
        }
    }
}
