package controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfExporter — writes a real, valid PDF file using nothing but the JDK.
 *
 * Why hand-rolled: the project has no third-party PDF library (iText / PDFBox)
 * on its classpath, and adding one would mean shipping an extra jar. A PDF is
 * just a structured text file, so this class emits one directly:
 *
 *   %PDF-1.4 header
 *   -> object 1  : /Catalog          (the document root)
 *   -> object 2  : /Pages            (the page tree)
 *   -> object 3,4: /Font             (Helvetica + Helvetica-Bold, Type1 base14)
 *   -> object 5,7,9... : /Page       (one per printed page)
 *   -> object 6,8,10...: content     (the actual drawing commands for that page)
 *   xref table (byte offset of every object) + trailer + %%EOF
 *
 * Drawing commands used are the standard PDF operators:
 *   "r g b rg"          set fill colour       "x y w h re f"  fill a rectangle
 *   "BT /F1 10 Tf ... ET"  a text block       "Tj"            show a string
 *
 * NOTE: PDF's origin is the BOTTOM-left corner. Everything in this class is
 * written top-down (easier to reason about) and converted in {@link #ty(float)}.
 */
public final class PdfExporter {

    private PdfExporter() {}

    // ---- Page geometry: A4 landscape, in PDF points (1pt = 1/72 inch) -------
    private static final float PAGE_W = 842f;
    private static final float PAGE_H = 595f;
    private static final float MARGIN = 34f;

    private static final float ROW_H       = 18f;
    private static final float HEAD_ROW_H  = 22f;
    private static final float BOTTOM_LIMIT = PAGE_H - MARGIN - 26f; // leave room for footer

    // ---- Brand colours (kept in sync with View.Theme) ----------------------
    private static final float[] C_DARK   = rgb(18, 48, 58);
    private static final float[] C_TEAL   = rgb(11, 114, 133);
    private static final float[] C_WHITE  = rgb(255, 255, 255);
    private static final float[] C_TEXT   = rgb(38, 54, 62);
    private static final float[] C_MUTED  = rgb(110, 130, 140);
    private static final float[] C_STRIPE = rgb(243, 248, 249);
    private static final float[] C_LINE   = rgb(214, 228, 232);

    private static float[] rgb(int r, int g, int b) {
        return new float[]{ r / 255f, g / 255f, b / 255f };
    }

    // ======================================================================
    // Public entry point
    // ======================================================================

    /**
     * Writes a full report — title band, summary cards, and a paginated table —
     * to the given file.
     *
     * @param file      destination .pdf file
     * @param title     big title, e.g. "Booking Report"
     * @param subtitle  line under the title, e.g. the date range
     * @param summary   summary cards as {label, value} pairs (may be empty/null)
     * @param headers   table column headings
     * @param rows      table body; every row must have headers.length cells
     */
    public static void writeTableReport(File file, String title, String subtitle,
                                        List<String[]> summary,
                                        String[] headers, List<String[]> rows) throws IOException {

        if (headers == null || headers.length == 0)
            throw new IllegalArgumentException("Report needs at least one column");
        if (rows == null) rows = new ArrayList<>();
        if (summary == null) summary = new ArrayList<>();

        float usableW = PAGE_W - 2 * MARGIN;
        float[] colW = columnWidths(headers, rows, usableW);
        boolean[] rightAlign = detectNumericColumns(headers, rows);

        List<StringBuilder> pages = new ArrayList<>();
        StringBuilder c = newPage(pages);

        // ---- Page 1 masthead -------------------------------------------------
        float y = MARGIN;
        y = drawTitleBand(c, title, subtitle, y);
        y += 14f;
        if (!summary.isEmpty()) {
            y = drawSummaryCards(c, summary, y, usableW);
            y += 16f;
        }

        // ---- Table ------------------------------------------------------------
        y = drawTableHeader(c, headers, colW, rightAlign, y);

        int rowIndex = 0;
        for (String[] row : rows) {
            if (y + ROW_H > BOTTOM_LIMIT) {
                c = newPage(pages);
                y = MARGIN;
                y = drawContinuedBand(c, title, pages.size(), y);
                y += 10f;
                y = drawTableHeader(c, headers, colW, rightAlign, y);
            }
            drawRow(c, row, colW, rightAlign, y, rowIndex++);
            y += ROW_H;
        }

        if (rows.isEmpty()) {
            text(c, MARGIN + 8, y + 13, "F1", 10, C_MUTED,
                 "No records matched the selected filters.");
            y += ROW_H;
        }

        // closing rule under the table
        line(c, MARGIN, y, PAGE_W - MARGIN, y, C_LINE, 0.7f);

        // ---- Footers (added last, once the total page count is known) --------
        String stamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        for (int i = 0; i < pages.size(); i++) {
            StringBuilder pc = pages.get(i);
            float fy = PAGE_H - MARGIN + 4;
            line(pc, MARGIN, fy - 12, PAGE_W - MARGIN, fy - 12, C_LINE, 0.7f);
            text(pc, MARGIN, fy, "F1", 8, C_MUTED,
                 "Travel BP Sri Lanka  \u2022  Tourism Management System  \u2022  Generated " + stamp);
            String pg = "Page " + (i + 1) + " of " + pages.size();
            text(pc, PAGE_W - MARGIN - width(pg, "F1", 8), fy, "F1", 8, C_MUTED, pg);
        }

        writeDocument(file, pages);
    }

    // ======================================================================
    // Page furniture
    // ======================================================================

    private static StringBuilder newPage(List<StringBuilder> pages) {
        StringBuilder c = new StringBuilder();
        pages.add(c);
        return c;
    }

    /** Dark banner with the report title + subtitle. Returns the new y cursor. */
    private static float drawTitleBand(StringBuilder c, String title, String subtitle, float y) {
        float h = 54f;
        rect(c, MARGIN, y, PAGE_W - 2 * MARGIN, h, C_DARK);
        // teal accent stripe down the left edge
        rect(c, MARGIN, y, 5f, h, C_TEAL);

        text(c, MARGIN + 18, y + 24, "F2", 17, C_WHITE, title);
        if (subtitle != null && !subtitle.isBlank())
            text(c, MARGIN + 18, y + 42, "F1", 9.5f, rgb(178, 205, 212), subtitle);

        String brand = "TRAVEL BP SRI LANKA";
        text(c, PAGE_W - MARGIN - 18 - width(brand, "F2", 11), y + 24, "F2", 11, C_WHITE, brand);
        String sys = "Tourism Management System";
        text(c, PAGE_W - MARGIN - 18 - width(sys, "F1", 8.5f), y + 40, "F1", 8.5f, rgb(178, 205, 212), sys);

        return y + h;
    }

    /** Slimmer banner used at the top of pages 2+. */
    private static float drawContinuedBand(StringBuilder c, String title, int pageNo, float y) {
        float h = 26f;
        rect(c, MARGIN, y, PAGE_W - 2 * MARGIN, h, C_DARK);
        rect(c, MARGIN, y, 5f, h, C_TEAL);
        text(c, MARGIN + 16, y + 17, "F2", 11, C_WHITE, title + "  (continued)");
        String brand = "TRAVEL BP SRI LANKA";
        text(c, PAGE_W - MARGIN - 16 - width(brand, "F1", 9), y + 17, "F1", 9, rgb(178, 205, 212), brand);
        return y + h;
    }

    /** A row of KPI cards ({label, value} pairs) across the full page width. */
    private static float drawSummaryCards(StringBuilder c, List<String[]> summary,
                                          float y, float usableW) {
        int n = summary.size();
        float gap = 12f;
        float cardW = (usableW - gap * (n - 1)) / n;
        float cardH = 46f;

        for (int i = 0; i < n; i++) {
            float x = MARGIN + i * (cardW + gap);
            rect(c, x, y, cardW, cardH, C_STRIPE);
            rect(c, x, y, 3.5f, cardH, C_TEAL);
            strokeRect(c, x, y, cardW, cardH, C_LINE, 0.7f);

            String label = summary.get(i)[0];
            String value = summary.get(i)[1];
            text(c, x + 12, y + 17, "F1", 8.5f, C_MUTED, fit(label, "F1", 8.5f, cardW - 22));
            text(c, x + 12, y + 36, "F2", 14, C_DARK, fit(value, "F2", 14, cardW - 22));
        }
        return y + cardH;
    }

    private static float drawTableHeader(StringBuilder c, String[] headers, float[] colW,
                                         boolean[] rightAlign, float y) {
        rect(c, MARGIN, y, PAGE_W - 2 * MARGIN, HEAD_ROW_H, C_DARK);
        float x = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            String h = fit(headers[i], "F2", 9, colW[i] - 14);
            // Headers follow their column's alignment, so a right-aligned
            // number column doesn't end up with a left-aligned caption.
            float tx = rightAlign[i]
                     ? x + colW[i] - 7 - width(h, "F2", 9)
                     : x + 7;
            text(c, tx, y + 15, "F2", 9, C_WHITE, h);
            x += colW[i];
        }
        return y + HEAD_ROW_H;
    }

    private static void drawRow(StringBuilder c, String[] row, float[] colW,
                                boolean[] rightAlign, float y, int index) {
        if (index % 2 == 1)
            rect(c, MARGIN, y, PAGE_W - 2 * MARGIN, ROW_H, C_STRIPE);

        float x = MARGIN;
        for (int i = 0; i < colW.length; i++) {
            String cell = (row != null && i < row.length && row[i] != null) ? row[i] : "";
            cell = fit(cell, "F1", 9, colW[i] - 14);
            float tx = rightAlign[i]
                     ? x + colW[i] - 7 - width(cell, "F1", 9)
                     : x + 7;
            text(c, tx, y + 12.5f, "F1", 9, C_TEXT, cell);
            x += colW[i];
        }
        line(c, MARGIN, y + ROW_H, PAGE_W - MARGIN, y + ROW_H, C_LINE, 0.4f);
    }

    // ======================================================================
    // Column sizing
    // ======================================================================

    /** Sizes columns to their natural content width, then scales to fit the page. */
    private static float[] columnWidths(String[] headers, List<String[]> rows, float usableW) {
        int n = headers.length;
        float[] natural = new float[n];

        for (int i = 0; i < n; i++)
            natural[i] = width(headers[i], "F2", 9) + 18;

        int sampled = 0;
        for (String[] r : rows) {
            if (sampled++ > 400) break; // don't scan enormous result sets
            for (int i = 0; i < n && i < r.length; i++) {
                String v = r[i] == null ? "" : r[i];
                natural[i] = Math.max(natural[i], width(v, "F1", 9) + 18);
            }
        }

        float min = 42f, max = usableW * 0.34f;
        float total = 0;
        for (int i = 0; i < n; i++) {
            natural[i] = Math.min(max, Math.max(min, natural[i]));
            total += natural[i];
        }

        float scale = usableW / total;   // stretch narrow tables, squeeze wide ones
        for (int i = 0; i < n; i++) natural[i] *= scale;
        return natural;
    }

    /** A column is right-aligned when most of its values look like numbers/money. */
    private static boolean[] detectNumericColumns(String[] headers, List<String[]> rows) {
        int n = headers.length;
        boolean[] out = new boolean[n];
        if (rows.isEmpty()) return out;

        for (int i = 0; i < n; i++) {
            int seen = 0, numeric = 0;
            for (String[] r : rows) {
                if (i >= r.length || r[i] == null || r[i].isBlank()) continue;
                seen++;
                if (looksNumeric(r[i])) numeric++;
                if (seen >= 40) break;
            }
            out[i] = seen > 0 && numeric >= seen * 0.8;
        }
        return out;
    }

    private static boolean looksNumeric(String s) {
        String t = s.replace("Rs.", "").replace("LKR", "").replace(",", "")
                    .replace("%", "").trim();
        if (t.isEmpty()) return false;
        try { Double.parseDouble(t); return true; }
        catch (NumberFormatException e) { return false; }
    }

    // ======================================================================
    // Low-level content-stream operators
    // ======================================================================

    /** Converts a top-down y coordinate into PDF's bottom-up space. */
    private static float ty(float yTopDown) { return PAGE_H - yTopDown; }

    private static void rect(StringBuilder c, float x, float y, float w, float h, float[] color) {
        c.append(fmt(color[0])).append(' ').append(fmt(color[1])).append(' ')
         .append(fmt(color[2])).append(" rg\n")
         .append(fmt(x)).append(' ').append(fmt(ty(y + h))).append(' ')
         .append(fmt(w)).append(' ').append(fmt(h)).append(" re f\n");
    }

    private static void strokeRect(StringBuilder c, float x, float y, float w, float h,
                                   float[] color, float lw) {
        c.append(fmt(color[0])).append(' ').append(fmt(color[1])).append(' ')
         .append(fmt(color[2])).append(" RG\n")
         .append(fmt(lw)).append(" w\n")
         .append(fmt(x)).append(' ').append(fmt(ty(y + h))).append(' ')
         .append(fmt(w)).append(' ').append(fmt(h)).append(" re S\n");
    }

    private static void line(StringBuilder c, float x1, float y1, float x2, float y2,
                             float[] color, float lw) {
        c.append(fmt(color[0])).append(' ').append(fmt(color[1])).append(' ')
         .append(fmt(color[2])).append(" RG\n")
         .append(fmt(lw)).append(" w\n")
         .append(fmt(x1)).append(' ').append(fmt(ty(y1))).append(" m ")
         .append(fmt(x2)).append(' ').append(fmt(ty(y2))).append(" l S\n");
    }

    private static void text(StringBuilder c, float x, float yBaseline, String font,
                             float size, float[] color, String s) {
        if (s == null || s.isEmpty()) return;
        c.append("BT /").append(font).append(' ').append(fmt(size)).append(" Tf\n")
         .append(fmt(color[0])).append(' ').append(fmt(color[1])).append(' ')
         .append(fmt(color[2])).append(" rg\n")
         .append(fmt(x)).append(' ').append(fmt(ty(yBaseline))).append(" Td\n")
         .append('(').append(escape(s)).append(") Tj ET\n");
    }

    private static String fmt(float v) {
        if (v == Math.rint(v)) return String.valueOf((int) v);
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    /** Escapes a string for a PDF literal, mapping anything outside WinAnsi to '?'. */
    private static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '(':  b.append("\\("); break;
                case ')':  b.append("\\)"); break;
                case '\\': b.append("\\\\"); break;
                case '\n':
                case '\r':
                case '\t': b.append(' '); break;
                default:
                    if (ch == '\u2022')      b.append("\\225");            // bullet
                    else if (ch == '\u2013' || ch == '\u2014') b.append("\\226"); // dashes
                    else if (ch < 32)        b.append(' ');
                    else if (ch < 127)       b.append(ch);
                    else if (ch <= 255)      b.append('\\').append(Integer.toOctalString(ch));
                    else                     b.append('?');
            }
        }
        return b.toString();
    }

    // ======================================================================
    // Helvetica metrics — so columns and alignment are actually accurate
    // ======================================================================

    /** Advance widths (per 1000 units) for printable ASCII 32..126, Helvetica. */
    private static final int[] W_REG = {
        278,278,355,556,556,889,667,191,333,333,389,584,278,333,278,278,
        556,556,556,556,556,556,556,556,556,556,278,278,584,584,584,556,
        1015,667,667,722,722,667,611,778,722,278,500,667,556,833,722,778,
        667,778,722,667,611,722,667,944,667,667,611,278,278,278,469,556,
        333,556,556,500,556,556,278,556,556,222,222,500,222,833,556,556,
        556,556,333,500,278,556,500,722,500,500,500,334,260,334,584
    };
    /** Same table for Helvetica-Bold. */
    private static final int[] W_BOLD = {
        278,333,474,556,556,889,722,238,333,333,389,584,278,333,278,278,
        556,556,556,556,556,556,556,556,556,556,333,333,584,584,584,611,
        975,722,722,722,722,667,611,778,722,278,556,722,611,833,722,778,
        667,778,722,667,611,722,667,944,667,667,611,333,278,333,584,556,
        333,556,611,556,611,556,333,611,611,278,278,556,278,889,611,611,
        611,611,389,556,333,611,556,778,556,556,500,389,280,389,584
    };

    /** Width of a string in points at the given font/size. */
    private static float width(String s, String font, float size) {
        if (s == null || s.isEmpty()) return 0;
        int[] w = "F2".equals(font) ? W_BOLD : W_REG;
        int units = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch <= 126) units += w[ch - 32];
            else units += 556; // reasonable fallback for anything else
        }
        return units * size / 1000f;
    }

    /** Truncates with an ellipsis so a cell never spills into its neighbour. */
    private static String fit(String s, String font, float size, float maxW) {
        if (s == null) return "";
        if (maxW <= 0) return "";
        if (width(s, font, size) <= maxW) return s;
        String ell = "...";
        float ew = width(ell, font, size);
        StringBuilder b = new StringBuilder();
        float acc = 0;
        for (int i = 0; i < s.length(); i++) {
            float cw = width(String.valueOf(s.charAt(i)), font, size);
            if (acc + cw + ew > maxW) break;
            b.append(s.charAt(i));
            acc += cw;
        }
        return b.append(ell).toString();
    }

    // ======================================================================
    // Document assembly: objects, xref table, trailer
    // ======================================================================

    private static void writeDocument(File file, List<StringBuilder> pages) throws IOException {
        int pageCount = pages.size();

        // Fixed object numbering:
        //   1 catalog, 2 pages, 3 F1, 4 F2,
        //   page i -> 5 + 2*(i-1), its content -> 6 + 2*(i-1)
        int totalObjects = 4 + pageCount * 2;

        List<byte[]> bodies = new ArrayList<>();

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            if (i > 0) kids.append(' ');
            kids.append(5 + 2 * i).append(" 0 R");
        }

        bodies.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
        bodies.add(ascii("<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>"));
        bodies.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        bodies.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));

        for (int i = 0; i < pageCount; i++) {
            int contentObj = 6 + 2 * i;
            bodies.add(ascii(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + fmt(PAGE_W) + " " + fmt(PAGE_H) + "] " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
                "/Contents " + contentObj + " 0 R >>"));

            byte[] stream = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream ob = new ByteArrayOutputStream();
            ob.write(ascii("<< /Length " + stream.length + " >>\nstream\n"));
            ob.write(stream);
            ob.write(ascii("\nendstream"));
            bodies.add(ob.toByteArray());
        }

        // ---- Serialise, remembering the byte offset of every object ----------
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long[] offsets = new long[totalObjects + 1];

        out.write(ascii("%PDF-1.4\n"));
        // binary comment line: marks the file as containing binary data
        out.write(new byte[]{ '%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n' });

        for (int i = 0; i < bodies.size(); i++) {
            int objNo = i + 1;
            offsets[objNo] = out.size();
            out.write(ascii(objNo + " 0 obj\n"));
            out.write(bodies.get(i));
            out.write(ascii("\nendobj\n"));
        }

        long xrefStart = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(totalObjects + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= totalObjects; i++)
            xref.append(String.format("%010d 00000 n \n", offsets[i]));
        out.write(ascii(xref.toString()));

        out.write(ascii("trailer\n<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>\n"
                      + "startxref\n" + xrefStart + "\n%%EOF\n"));

        try (OutputStream fos = new FileOutputStream(file)) {
            fos.write(out.toByteArray());
        }
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }
}
