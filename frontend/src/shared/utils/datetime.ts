/**
 * DateTime handling utilities
 *
 * PROBLEM: HTML datetime-local inputs don't include timezone info.
 * Naive conversion to ISO string shifts time based on local timezone.
 *
 * SOLUTION: Send the local datetime string as-is to backend.
 * Backend interprets it in local timezone, not UTC.
 *
 * USAGE:
 * - Use getLocalDateTimeString() to extract datetime-local value
 * - Pass it directly to API endpoints
 * - Backend should NOT call toISOString(), just store as LocalDateTime
 *
 * If backend MUST use ISO format:
 * - Use toLocalISOString() to convert, then add timezone offset
 * - Document format: "YYYY-MM-DDTHH:mm:ss.sssZ" with local time in the Z position
 */

/**
 * Convert HTML datetime-local input value to ISO string WITHOUT timezone shift
 *
 * @example
 * // Input: datetime-local value "2026-02-28T14:30:00"
 * // Output: "2026-02-28T14:30:00.000Z" (no timezone conversion)
 * toLocalISOString("2026-02-28T14:30:00");
 */
export function toLocalISOString(datetimeLocalValue: string): string {
  if (!datetimeLocalValue) return "";

  // datetime-local format is already: YYYY-MM-DDTHH:mm
  // Just ensure milliseconds are included
  const parts = datetimeLocalValue.split("T");
  if (parts.length !== 2) return datetimeLocalValue;

  const [date, time] = parts;
  const [hours, minutes, seconds = "00"] = time.split(":");

  // Format: YYYY-MM-DDTHH:mm:ss.000Z (local time, no tz conversion)
  return `${date}T${hours}:${minutes}:${seconds}.000Z`;
}

/**
 * Get datetime-local value from an input element
 * Returns the string as-is (format: YYYY-MM-DDTHH:mm:ss)
 */
export function getLocalDateTimeString(inputElement: HTMLInputElement): string {
  return inputElement.value; // Already in correct format
}

/**
 * Parse datetime-local string to Date object
 * WARNING: This will create a Date in UTC, so calculations will be off
 * Only use for display/comparisons that don't depend on actual time
 *
 * BETTER: Keep as string and send to backend
 */
export function parseLocalDateTime(datetimeLocalValue: string): Date {
  // This is a workaround; better to avoid Date objects and keep strings
  return new Date(`${datetimeLocalValue}Z`);
}

/**
 * Format Date to datetime-local input value
 * Converts local date to YYYY-MM-DDTHH:mm format
 *
 * @example
 * formatToLocalDateTime(new Date()) // "2026-02-28T14:30"
 */
export function formatToLocalDateTime(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

/**
 * Validate datetime range
 * Ensures start < end
 */
export function isValidDateTimeRange(
  startValue: string,
  endValue: string,
): boolean {
  if (!startValue || !endValue) return false;

  const start = new Date(`${startValue}Z`);
  const end = new Date(`${endValue}Z`);

  return start < end;
}

/**
 * BACKEND INTEGRATION NOTES:
 *
 * Current Problem:
 * - Frontend sends: "2026-02-28T14:30:00.000Z"
 * - Backend reads as: UTC 14:30, but user meant LOCAL 14:30
 * - Result: Time is shifted by local timezone offset
 *
 * Solutions (choose one):
 *
 * 1. BEST: Store local datetime + timezone separately
 *    - Frontend: Send { time: "2026-02-28T14:30:00", tzOffset: -300 }
 *    - Backend: Parse as LocalDateTime + ZoneOffset
 *    - Java: ZonedDateTime.of(LocalDateTime, ZoneId)
 *
 * 2. SIMPLE: Backend ignores timezone info
 *    - Frontend: Send "2026-02-28T14:30:00" (raw string, no .000Z)
 *    - Backend: new Timestamp(LocalDateTime.parse(value))
 *    - Java: LocalDateTime.parse("2026-02-28T14:30:00")
 *    - Ensure backend interprets as local, not UTC
 *
 * 3. CLIENT LIBRARY: Use date-fns-tz or luxon
 *    - import { formatToTimeZone } from 'date-fns-tz'
 *    - Handles all timezone conversions automatically
 */
