"use client";

import { useEffect } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { TextField, TextAreaField } from "@/components/ui/field";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { useToast } from "@/components/ui/toast";
import { useCreateKnowledgeBase } from "@/lib/hooks/use-knowledge-bases";
import {
  createKnowledgeBaseSchema,
  type CreateKnowledgeBaseValues,
} from "@/lib/validation/schemas";
import { applyFieldErrors } from "@/lib/utils/form-errors";
import type { KnowledgeBase } from "@/lib/types/api";

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated?: (kb: KnowledgeBase) => void;
}

export function CreateKnowledgeBaseDialog({ open, onClose, onCreated }: Props) {
  const toast = useToast();
  const mutation = useCreateKnowledgeBase();

  const {
    register,
    handleSubmit,
    reset,
    setError,
    control,
    formState: { errors },
  } = useForm<CreateKnowledgeBaseValues>({
    resolver: zodResolver(createKnowledgeBaseSchema),
    defaultValues: { name: "", description: "" },
  });

  useEffect(() => {
    if (open) {
      reset({ name: "", description: "" });
      mutation.reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const descriptionValue = useWatch({ control, name: "description" }) ?? "";

  const onSubmit = handleSubmit((values) => {
    mutation.mutate(
      {
        name: values.name.trim(),
        description: values.description?.trim() ? values.description.trim() : undefined,
      },
      {
        onSuccess: (kb) => {
          toast.success("Knowledge base created", kb.name);
          onCreated?.(kb);
          onClose();
        },
        onError: (err) => {
          applyFieldErrors(err, setError);
        },
      },
    );
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create knowledge base"
      description="Give it a name and an optional description. You can upload Documents once it exists."
      busy={mutation.isPending}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={mutation.isPending}>
            Cancel
          </Button>
          <Button type="submit" form="create-kb-form" loading={mutation.isPending}>
            Create
          </Button>
        </>
      }
    >
      <form id="create-kb-form" onSubmit={onSubmit} className="space-y-4" noValidate>
        {mutation.isError && !mutation.error.fieldErrors && (
          <ApiErrorMessage
            error={mutation.error}
            title="Could not create knowledge base"
            onRetry={() => onSubmit()}
            retrying={mutation.isPending}
          />
        )}

        <TextField
          label="Name"
          placeholder="Name of the knowledge base"
          maxLength={255}
          autoFocus
          error={errors.name?.message}
          {...register("name")}
        />

        <TextAreaField
          label="Description"
          optional
          placeholder="Description of the knowledge base"
          maxLength={4000}
          rows={4}
          hint={`${descriptionValue.length}/4000`}
          error={errors.description?.message}
          {...register("description")}
        />
      </form>
    </Modal>
  );
}
