package com.example.backend.modules.production.productionstock.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class QRCodeGenerator {

    /**
     * Generates a QR code image as a byte array.
     *
     * @param text   The content to encode in the QR code.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A byte array representing the PNG image of the QR code.
     */
    public byte[] generateQRCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Impossible de générer le QR code.", e);
        }
    }

    /**
     * Helper to join fields with a separator.
     *
     * @param fields The fields to join.
     * @return A joined string using "$" as separator.
     */
    public String joinFields(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i] != null ? fields[i].toString() : "");
            if (i < fields.length - 1) {
                sb.append("$");
            }
        }
        return sb.toString();
    }
}
