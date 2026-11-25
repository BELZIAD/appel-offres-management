import { useEffect, useState } from 'react';
import { Download, Plus, Trash2 } from 'lucide-react';
import { api, errorMessage } from '../../api/client';
import { BackupItem } from '../../api/types';
import { Toast } from '../../ui/Toast';

export function BackupsPage() {
  const [backups, setBackups] = useState<BackupItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  async function load() {
    const response = await api.get<{ backups: BackupItem[] }>('/admin/backups');
    setBackups(response.data.backups);
  }

  useEffect(() => {
    load();
  }, []);

  async function create() {
    setLoading(true);
    try {
      await api.post('/admin/backups');
      await load();
      setToast({ type: 'success', message: 'Sauvegarde creee' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  async function download(name: string) {
    const response = await api.get(`/admin/backups/${encodeURIComponent(name)}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function remove(name: string) {
    try {
      await api.delete(`/admin/backups/${encodeURIComponent(name)}`);
      await load();
      setToast({ type: 'success', message: 'Sauvegarde supprimee' });
    } catch (err) {
      setToast({ type: 'error', message: errorMessage(err) });
    }
  }

  return (
    <div className="space-y-5">
      {toast && <Toast type={toast.type} message={toast.message} />}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Sauvegardes</h1>
          <p className="text-sm text-muted">Dumps PostgreSQL verifies depuis l'administration.</p>
        </div>
        <button className="btn-primary" onClick={create} disabled={loading}><Plus size={16} /> {loading ? 'Creation...' : 'Creer une sauvegarde'}</button>
      </div>
      <section className="panel overflow-x-auto">
        <table className="table">
          <thead><tr><th className="th">Nom</th><th className="th">Statut</th><th className="th">Taille</th><th className="th">Creation</th><th className="th">Checksum</th><th className="th">Actions</th></tr></thead>
          <tbody>
            {backups.map(backup => (
              <tr key={backup.name}>
                <td className="td font-medium">{backup.name}</td>
                <td className="td">{backup.status}</td>
                <td className="td">{Math.max(1, Math.round(backup.size / 1024))} KB</td>
                <td className="td">{new Date(backup.createdAt).toLocaleString('fr-FR')}</td>
                <td className="td font-mono text-xs">{backup.checksumSha256 ? `${backup.checksumSha256.slice(0, 12)}...` : '-'}</td>
                <td className="td">
                  <div className="flex gap-2">
                    <button className="btn-secondary" onClick={() => download(backup.name)}><Download size={16} /> Telecharger</button>
                    <button className="btn-danger" onClick={() => remove(backup.name)}><Trash2 size={16} /> Supprimer</button>
                  </div>
                </td>
              </tr>
            ))}
            {backups.length === 0 && <tr><td className="td text-muted" colSpan={6}>Aucune sauvegarde.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}
