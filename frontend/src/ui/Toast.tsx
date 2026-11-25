import { CheckCircle2, XCircle } from 'lucide-react';

export function Toast({ message, type }: { message: string; type: 'success' | 'error' }) {
  return (
    <div className={`fixed right-5 top-5 z-50 flex items-center gap-2 rounded-md px-4 py-3 text-sm text-white shadow-lg ${type === 'success' ? 'bg-emerald-700' : 'bg-red-700'}`}>
      {type === 'success' ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
      {message}
    </div>
  );
}
