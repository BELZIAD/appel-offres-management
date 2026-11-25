import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { TenderPricing } from '../../api/types';
import { formatMoney, pricingStatusLabels } from '../../utils/labels';

export function EmployeePricingPage() {
  const [items, setItems] = useState<TenderPricing[]>([]);
  useEffect(() => { api.get<TenderPricing[]>('/employee/pricing').then(r => setItems(r.data)); }, []);
  return <div className="space-y-5"><h1 className="text-2xl font-semibold">Mes chiffrages</h1><section className="panel overflow-x-auto"><table className="table"><thead><tr><th className="th">AO</th><th className="th">Etat</th><th className="th">Total</th><th className="th">Actions</th></tr></thead><tbody>{items.map(p => <tr key={p.id}><td className="td">#{p.tenderId}</td><td className="td">{pricingStatusLabels[p.status]}</td><td className="td">{formatMoney(p.finalTotal)}</td><td className="td"><Link className="btn-secondary" to={`/employee/tenders/${p.tenderId}/pricing`}>Ouvrir</Link></td></tr>)}</tbody></table>{items.length === 0 && <div className="py-8 text-center text-sm text-muted">Aucun chiffrage affecte.</div>}</section></div>;
}
