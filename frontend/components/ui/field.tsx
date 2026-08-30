import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type TextareaHTMLAttributes,
  type ReactNode,
} from "react";
import { cn } from "@/lib/utils/cn";
import { FieldError } from "@/components/errors/field-error";

interface FieldWrapProps {
  label: string;
  htmlFor: string;
  error?: string;
  hint?: ReactNode;
  optional?: boolean;
  children: ReactNode;
}

export function FieldWrap({ label, htmlFor, error, hint, optional, children }: FieldWrapProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-slate-800">
        {label}
        {optional && <span className="ml-1 font-normal text-slate-400">(optional)</span>}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-slate-500">{hint}</p>}
      <FieldError message={error} id={`${htmlFor}-error`} />
    </div>
  );
}

const baseControl =
  "block w-full rounded-md border bg-white px-3 py-2 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 focus:outline-2 focus:outline-offset-0 focus:outline-slate-900 disabled:cursor-not-allowed disabled:bg-slate-50";

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: ReactNode;
  optional?: boolean;
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, hint, optional, id, className, ...rest },
  ref,
) {
  const reactId = useId();
  const fieldId = id ?? reactId;
  return (
    <FieldWrap label={label} htmlFor={fieldId} error={error} hint={hint} optional={optional}>
      <input
        ref={ref}
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        className={cn(baseControl, error ? "border-red-400" : "border-slate-300", className)}
        {...rest}
      />
    </FieldWrap>
  );
});

export interface TextAreaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
  hint?: ReactNode;
  optional?: boolean;
}

export const TextAreaField = forwardRef<HTMLTextAreaElement, TextAreaFieldProps>(
  function TextAreaField({ label, error, hint, optional, id, className, ...rest }, ref) {
    const reactId = useId();
  const fieldId = id ?? reactId;
    return (
      <FieldWrap label={label} htmlFor={fieldId} error={error} hint={hint} optional={optional}>
        <textarea
          ref={ref}
          id={fieldId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${fieldId}-error` : undefined}
          className={cn(
            baseControl,
            "min-h-[90px]",
            error ? "border-red-400" : "border-slate-300",
            className,
          )}
          {...rest}
        />
      </FieldWrap>
    );
  },
);
