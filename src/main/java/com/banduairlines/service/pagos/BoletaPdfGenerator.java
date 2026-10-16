package com.banduairlines.service.pagos;

import org.springframework.stereotype.Service;

import com.banduairlines.dto.pagos.BoletaResponse;
import com.banduairlines.dto.pagos.LineaBoletaDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BoletaPdfGenerator {

    public byte[] generarBoletaPdf(BoletaResponse boleta) {
        try {
            ByteArrayOutputStream streamContent = new ByteArrayOutputStream();
            StringBuilder textStream = new StringBuilder();

            textStream.append("BT\n");
            textStream.append("/F1 16 Tf\n");
            textStream.append("50 750 Td\n");
            textStream.append("(").append(escapePdf(boleta.emisor().razonSocial())).append(") Tj\n");

            textStream.append("/F1 10 Tf\n");
            textStream.append("0 -15 Td\n");
            textStream.append("(RUT: ").append(escapePdf(boleta.emisor().rut())).append(" - Giro: ").append(escapePdf(boleta.emisor().giro())).append(") Tj\n");
            textStream.append("0 -13 Td\n");
            textStream.append("(Direccion: ").append(escapePdf(boleta.emisor().direccion())).append(") Tj\n");
            textStream.append("0 -13 Td\n");
            textStream.append("(Telefono: ").append(escapePdf(boleta.emisor().telefono())).append(") Tj\n");

            textStream.append("/F1 14 Tf\n");
            textStream.append("0 -25 Td\n");
            textStream.append("(BOLETA ELECTRONICA Nro. ").append(boleta.numeroBoleta()).append(") Tj\n");

            textStream.append("/F1 10 Tf\n");
            textStream.append("0 -15 Td\n");
            textStream.append("(Fecha Emision: ").append(boleta.fechaEmision() != null ? boleta.fechaEmision().toString() : "").append(") Tj\n");
            textStream.append("0 -13 Td\n");
            textStream.append("(Codigo de Reserva (PNR): ").append(escapePdf(boleta.codigoReserva())).append(") Tj\n");

            textStream.append("0 -18 Td\n");
            textStream.append("(RECEPTOR / CLIENTE) Tj\n");
            textStream.append("0 -13 Td\n");
            textStream.append("(Senor(a): ").append(escapePdf(boleta.receptor() != null ? boleta.receptor().nombreCompleto() : "Pasajero")).append(") Tj\n");
            if (boleta.receptor() != null && boleta.receptor().rut() != null && !boleta.receptor().rut().isBlank()) {
                textStream.append("0 -13 Td\n");
                textStream.append("(RUT: ").append(escapePdf(boleta.receptor().rut())).append(") Tj\n");
            }

            textStream.append("0 -18 Td\n");
            textStream.append("(PASAJEROS:) Tj\n");
            if (boleta.pasajeros() != null) {
                for (String p : boleta.pasajeros()) {
                    textStream.append("0 -13 Td\n");
                    textStream.append("(- ").append(escapePdf(p)).append(") Tj\n");
                }
            }

            textStream.append("0 -20 Td\n");
            textStream.append("-------------------------------------------------------------------------------- Tj\n");
            textStream.append("0 -15 Td\n");
            textStream.append("(DETALLE DE COMPRA (Precios con IVA incluido)) Tj\n");
            textStream.append("0 -15 Td\n");

            if (boleta.lineas() != null) {
                for (LineaBoletaDto linea : boleta.lineas()) {
                    String desc = linea.descripcion() != null ? linea.descripcion() : linea.concepto();
                    textStream.append("(").append(escapePdf(desc)).append("   $").append(linea.montoCLP()).append(" CLP) Tj\n");
                    textStream.append("0 -14 Td\n");
                }
            }

            if (boleta.descuentoCLP() != null && boleta.descuentoCLP() > 0) {
                textStream.append("(Descuento Aplicado: -$").append(boleta.descuentoCLP()).append(" CLP) Tj\n");
                textStream.append("0 -14 Td\n");
            }

            textStream.append("-------------------------------------------------------------------------------- Tj\n");
            textStream.append("/F1 12 Tf\n");
            textStream.append("0 -16 Td\n");
            textStream.append("(TOTAL PAGADO: $").append(boleta.totalCLP()).append(" CLP) Tj\n");

            textStream.append("/F1 10 Tf\n");
            textStream.append("0 -20 Td\n");
            if (boleta.pago() != null) {
                String medio = boleta.pago().medioPago() != null ? boleta.pago().medioPago().nombre() : "Flow";
                String digits = (boleta.pago().medioPago() != null && boleta.pago().medioPago().ultimosDigitos() != null)
                        ? " ****" + boleta.pago().medioPago().ultimosDigitos() : "";
                textStream.append("(Forma de Pago: ").append(escapePdf(medio + digits)).append(") Tj\n");
                textStream.append("0 -13 Td\n");
                textStream.append("(Orden Flow: FL-").append(boleta.pago().ordenFlow() != null ? boleta.pago().ordenFlow() : "").append(") Tj\n");
                textStream.append("0 -13 Td\n");
            }

            textStream.append("0 -30 Td\n");
            textStream.append("/F1 8 Tf\n");
            textStream.append("(Documento Tributario Electronico generado por BanduAirlines SpA) Tj\n");
            textStream.append("ET\n");

            byte[] streamBytes = textStream.toString().getBytes(StandardCharsets.ISO_8859_1);

            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            pdfOut.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 1: Catalog
            offsets.add(pdfOut.size());
            pdfOut.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 2: Pages
            offsets.add(pdfOut.size());
            pdfOut.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 3: Page
            offsets.add(pdfOut.size());
            pdfOut.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 4: Contents Stream
            offsets.add(pdfOut.size());
            String streamHeader = "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
            pdfOut.write(streamHeader.getBytes(StandardCharsets.ISO_8859_1));
            pdfOut.write(streamBytes);
            pdfOut.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 5: Font Helvetica
            offsets.add(pdfOut.size());
            pdfOut.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Xref
            int startXref = pdfOut.size();
            pdfOut.write("xref\n0 6\n0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                pdfOut.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }

            // Trailer
            pdfOut.write("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n".getBytes(StandardCharsets.ISO_8859_1));
            pdfOut.write(String.valueOf(startXref).getBytes(StandardCharsets.ISO_8859_1));
            pdfOut.write("\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1));

            return pdfOut.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF de la boleta", e);
        }
    }

    private String escapePdf(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("\r", "")
                   .replace("\n", " ");
    }
}
