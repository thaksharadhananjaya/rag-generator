/** Inline validation message shown beneath a form control. */
export function FieldError({ message, id }: { message?: string; id?: string }) {
  if (!message) return null;
  return (
    <p id={id} role="alert" className="text-xs font-medium text-red-600">
      {message}
    </p>
  );
}
