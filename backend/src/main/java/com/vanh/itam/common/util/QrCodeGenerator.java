package com.vanh.itam.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * ZXing wrapper để sinh QR code PNG.
 * Content là asset.code — đủ để quét ra code, tra cứu hệ thống.
 */
@Component
public class QrCodeGenerator {

    /**
     * Sinh QR code PNG cho content đã cho.
     *
     * @param content nội dung encode vào QR (asset.code)
     * @param width   chiều rộng ảnh (px)
     * @param height  chiều cao ảnh (px)
     * @return byte array của ảnh PNG
     */
    public byte[] generate(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
