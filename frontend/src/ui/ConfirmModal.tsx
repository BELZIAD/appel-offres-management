import { AlertTriangle } from 'lucide-react';

export function ConfirmModal({ title, message, onCancel, onConfirm }: { title: string; message: string; onCancel: () => void; onConfirm: () => void }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/35 p-4">
      <div className="w-full max-w-md rounded-lg bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center gap-3">
          <AlertTriangle className="text-red-600" />
          <h2 className="text-lg font-semibold">{title}</h2>
        </div>
        <p className="text-sm text-slate-600">{message}</p>
        <div className="mt-5 flex justify-end gap-2">
          <button className="btn-secondary" onClick={onCancel}>Annuler</button>
          <button className="btn-danger" onClick={onConfirm}>Confirmer</button>
        </div>
      </div>
    </div>
  );
}
