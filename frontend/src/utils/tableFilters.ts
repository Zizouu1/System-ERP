export function normalizeFilterText(value: unknown): string {
  return String(value ?? "").trim().toLowerCase();
}

export function matchesAnyText(values: unknown[], query: string): boolean {
  const normalizedQuery = normalizeFilterText(query);
  if (!normalizedQuery) return true;
  return values.some((value) => normalizeFilterText(value).includes(normalizedQuery));
}

export function matchesText(value: unknown, query: string): boolean {
  const normalizedQuery = normalizeFilterText(query);
  if (!normalizedQuery) return true;
  return normalizeFilterText(value).includes(normalizedQuery);
}

export function matchesExact(value: unknown, selected: string): boolean {
  if (!selected) return true;
  return String(value ?? "") === selected;
}

export function matchesNumberMin(value: number | undefined | null, minValue: string): boolean {
  if (!minValue.trim()) return true;
  const min = Number(minValue);
  if (Number.isNaN(min)) return true;
  return Number(value ?? 0) >= min;
}

export function matchesNumberMax(value: number | undefined | null, maxValue: string): boolean {
  if (!maxValue.trim()) return true;
  const max = Number(maxValue);
  if (Number.isNaN(max)) return true;
  return Number(value ?? 0) <= max;
}

export function matchesDateRange(
  value: string | undefined | null,
  startDate: string,
  endDate: string,
): boolean {
  if (!startDate && !endDate) return true;
  if (!value) return false;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;

  const start = startDate ? new Date(`${startDate}T00:00:00`) : null;
  const end = endDate ? new Date(`${endDate}T23:59:59.999`) : null;

  if (start && date < start) return false;
  if (end && date > end) return false;
  return true;
}
