import { Modal } from "@/components/Modal";

type ConfirmationDialogProps = {
  open: boolean;
  title?: string;
  message: string;
  confirmLabel?: string;
  closeLabel?: string;
  onConfirm: () => void;
  onClose: () => void;
};

export function ConfirmationDialog({
  open,
  title = "Confirmation",
  message,
  confirmLabel = "Confirmer",
  closeLabel = "Fermer",
  onConfirm,
  onClose,
}: ConfirmationDialogProps) {
  return (
    <Modal
      open={open}
      title=""
      onClose={onClose}
      showDefaultCloseAction={false}
      width={460}
    >
      <div className="confirmationDialogContent">
        <h2 className="confirmationDialogTitle">{title}</h2>
        <p className="confirmationDialogMessage">{message}</p>
        <div className="confirmationDialogActions">
          <button
            className="btn btnPrimary confirmationDialogButton"
            onClick={onConfirm}
            type="button"
          >
            {confirmLabel}
          </button>
          <button
            className="btn btnGhost confirmationDialogButton"
            onClick={onClose}
            type="button"
          >
            {closeLabel}
          </button>
        </div>
      </div>
    </Modal>
  );
}
