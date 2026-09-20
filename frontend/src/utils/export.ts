export function exportToCsv(filename: string, rows: any[], columns: { header: string; cell: (r: any) => any }[]) {
    const headers = columns.map((c) => c.header).join(",");
    const data = rows.map((r) =>
        columns.map((c) => {
            const val = c.cell(r);
            // If cell returns a React element (badge), try to extract text or just use a placeholder
            let text = val;
            if (typeof val !== "string" && typeof val !== "number") {
                // If it's a React element like <span className="badge">123</span>, 
                // we prefer passing a clean getter. But for now, we'll try to handle basic props.
                if (val && val.props && val.props.children) {
                    text = val.props.children;
                } else {
                    text = "";
                }
            }
            return `"${String(text ?? "").replace(/"/g, '""')}"`;
        }).join(",")
    );

    const csvContent = "\uFEFF" + [headers, ...data].join("\n"); // Add BOM for Excel UTF-8
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

export function printTable() {
    window.print();
}
