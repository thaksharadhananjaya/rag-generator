import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { ApiError } from "@/lib/types/api";

/**
 * Map a normalized {@link ApiError}'s `fieldErrors` onto a React Hook Form.
 * Unknown field names are ignored (the caller still shows the top-level error).
 */
export function applyFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  knownFields?: Array<Path<T>>,
): boolean {
  if (!error.fieldErrors) return false;
  let applied = false;
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if (knownFields && !knownFields.includes(field as Path<T>)) continue;
    setError(field as Path<T>, { type: "server", message });
    applied = true;
  }
  return applied;
}
