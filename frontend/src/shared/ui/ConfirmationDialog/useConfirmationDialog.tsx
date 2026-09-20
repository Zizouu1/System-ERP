import { useCallback, useRef, useState } from "react";
import { ConfirmationDialog } from "./ConfirmationDialog";

type ConfirmationOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  closeLabel?: string;
};

type DialogState = ConfirmationOptions | null;

export function useConfirmationDialog() {
  const [dialog, setDialog] = useState<DialogState>(null);
  const resolverRef = useRef<((confirmed: boolean) => void) | null>(null);

  const closeDialog = useCallback((confirmed: boolean) => {
    const resolver = resolverRef.current;
    resolverRef.current = null;
    setDialog(null);
    resolver?.(confirmed);
  }, []);

  const askConfirmation = useCallback((options: ConfirmationOptions) => {
    return new Promise<boolean>((resolve) => {
      resolverRef.current = resolve;
      setDialog(options);
    });
  }, []);

  return {
    askConfirmation,
    confirmationDialog: (
      <ConfirmationDialog
        open={dialog !== null}
        title={dialog?.title}
        message={dialog?.message ?? ""}
        confirmLabel={dialog?.confirmLabel}
        closeLabel={dialog?.closeLabel}
        onConfirm={() => closeDialog(true)}
        onClose={() => closeDialog(false)}
      />
    ),
  };
}
