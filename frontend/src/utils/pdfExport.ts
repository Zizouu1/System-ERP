import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

export interface PdfColumn {
    header: string;
    getData: (row: any) => string;
}

export function exportToPdf(filename: string, title: string, rows: any[], columns: PdfColumn[]) {
    const doc = new jsPDF();

    // Add title
    doc.setFontSize(18);
    doc.text(title, 14, 22);
    doc.setFontSize(11);
    doc.setTextColor(100);

    // Add date
    const dateStr = new Date().toLocaleString("fr-FR");
    doc.text(`Généré le: ${dateStr}`, 14, 30);

    const head = [columns.map(c => c.header)];
    const body = rows.map(row => columns.map(c => c.getData(row)));

    autoTable(doc, {
        head,
        body,
        startY: 35,
        styles: { fontSize: 9, cellPadding: 3 },
        headStyles: { fillColor: [63, 81, 181], textColor: 255 }, // Premium indigo-like color
        alternateRowStyles: { fillColor: [245, 245, 245] },
        margin: { top: 35 },
    });

    doc.save(filename);
}
