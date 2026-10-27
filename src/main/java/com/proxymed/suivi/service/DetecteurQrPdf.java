package com.proxymed.suivi.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import com.proxymed.suivi.exception.RegleGestionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class DetecteurQrPdf {

    static final int RESOLUTION_DPI = 300;

    private static final Map<DecodeHintType, Object> INDICES = Map.of(
            DecodeHintType.TRY_HARDER, Boolean.TRUE,
            DecodeHintType.CHARACTER_SET, "UTF-8");

    public List<String> detecter(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer rendu = new PDFRenderer(document);
            List<String> textes = new ArrayList<>();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                textes.addAll(decoder(rendu.renderImageWithDPI(page, RESOLUTION_DPI, ImageType.GRAY)));
            }
            return textes;
        } catch (IOException e) {
            throw new RegleGestionException("Le fichier PDF est illisible ou endommage");
        }
    }

    private List<String> decoder(BufferedImage image) {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        for (BinaryBitmap bitmap : List.of(new BinaryBitmap(new HybridBinarizer(source)),
                new BinaryBitmap(new GlobalHistogramBinarizer(source)))) {
            try {
                // Tableau vide (et non exception) quand des motifs sont reperes sans pouvoir etre decodes.
                List<String> textes = Arrays.stream(new QRCodeMultiReader().decodeMultiple(bitmap, INDICES))
                        .map(Result::getText).distinct().toList();
                if (!textes.isEmpty()) {
                    return textes;
                }
            } catch (ReaderException e) {
                // Aucun QR trouve par le lecteur multiple : essai du lecteur simple.
            }
            try {
                return List.of(new QRCodeReader().decode(bitmap, INDICES).getText());
            } catch (ReaderException e) {
                // Pas de QR lisible avec ce binariseur : essai suivant.
            }
        }
        return List.of();
    }
}
