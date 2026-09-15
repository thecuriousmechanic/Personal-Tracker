import type { LocalDate } from "@/types/entities";

export function toLocalDate(date: Date): LocalDate {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function formatShortDate(value?: LocalDate): string {
  if (!value) return "No date";
  const [year, month, day] = value.split("-").map(Number);
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
  }).format(new Date(year, month - 1, day));
}

export function formatRelativeDate(value: LocalDate | undefined, today: LocalDate): string {
  if (!value) return "Inbox";
  if (value === today) return "Today";
  const tomorrow = addDays(new Date(`${today}T12:00:00`), 1);
  if (value === toLocalDate(tomorrow)) return "Tomorrow";
  return formatShortDate(value);
}
