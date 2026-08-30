import { z } from "zod";
import { config } from "@/lib/config";

/**
 * Zod validation schemas for every form in the app.
 * Bounds mirror the backend contract; backend validation stays authoritative.
 */

/* ------------------------------------------------------------------ */
/* Create knowledge base                                              */
/* ------------------------------------------------------------------ */

export const createKnowledgeBaseSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required.")
    .max(255, "Name must be at most 255 characters."),
  description: z
    .string()
    .trim()
    .max(4000, "Description must be at most 4000 characters.")
    .optional()
    .or(z.literal("")),
});

export type CreateKnowledgeBaseValues = z.infer<typeof createKnowledgeBaseSchema>;

/* ------------------------------------------------------------------ */
/* RAG query                                                          */
/* ------------------------------------------------------------------ */

const MAX_QUESTION = 4000;

/**
 * Advanced-option field: the form always holds a string. An empty string means
 * "not set" and becomes `undefined`; anything else is parsed and range-checked.
 */
function optionalNumberField(opts: {
  min: number;
  max: number;
  integer?: boolean;
  label: string;
}) {
  return z
    .string()
    .trim()
    .transform((v, ctx) => {
      if (v === "") return undefined;
      const n = Number(v);
      if (!Number.isFinite(n)) {
        ctx.addIssue({ code: "custom", message: `${opts.label} must be a number.` });
        return z.NEVER;
      }
      if (opts.integer && !Number.isInteger(n)) {
        ctx.addIssue({ code: "custom", message: `${opts.label} must be a whole number.` });
        return z.NEVER;
      }
      if (n < opts.min || n > opts.max) {
        ctx.addIssue({
          code: "custom",
          message: `${opts.label} must be between ${opts.min} and ${opts.max}.`,
        });
        return z.NEVER;
      }
      return n;
    });
}

export const ragQuerySchema = z.object({
  question: z
    .string()
    .trim()
    .min(1, "Enter a question.")
    .max(MAX_QUESTION, `Question must be at most ${MAX_QUESTION} characters.`),
  topK: optionalNumberField({ min: 0, max: 50, integer: true, label: "topK" }),
  minScore: optionalNumberField({ min: 0, max: 1, label: "minScore" }),
});

/** Shape the form holds (all advanced fields are strings). */
export type RagQueryFormValues = z.input<typeof ragQuerySchema>;
/** Shape after validation/transform (numbers or undefined). */
export type RagQueryValues = z.output<typeof ragQuerySchema>;

export const QUESTION_MAX_LENGTH = MAX_QUESTION;

/* ------------------------------------------------------------------ */
/* PDF upload (client-side guard only)                                */
/* ------------------------------------------------------------------ */

export interface FileValidationResult {
  ok: boolean;
  error?: string;
}

export function validatePdfFile(file: File | null | undefined): FileValidationResult {
  if (!file) {
    return { ok: false, error: "Select a file to upload." };
  }
  const nameOk = file.name.toLowerCase().endsWith(".pdf");
  const typeOk = file.type === "application/pdf" || file.type === "" || file.type === "application/x-pdf";
  if (!nameOk) {
    return { ok: false, error: "The file must have a .pdf extension." };
  }
  if (!typeOk) {
    return { ok: false, error: "Only PDF files are supported." };
  }
  if (file.size <= 0) {
    return { ok: false, error: "The selected file is empty." };
  }
  if (file.size > config.maxUploadBytes) {
    return {
      ok: false,
      error: `The file exceeds the ${config.maxUploadLabel} limit.`,
    };
  }
  return { ok: true };
}
