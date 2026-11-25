import { useEffect, useState } from 'react';
import { api, assetUrl } from '../api/client';
import { AboutInfo } from '../api/types';

export function AboutPage() {
  const [about, setAbout] = useState<AboutInfo | null>(null);

  useEffect(() => {
    api.get<AboutInfo>('/about').then(response => setAbout(response.data));
  }, []);

  if (!about) return <div className="panel">Chargement...</div>;
  const company = about.company;

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <section className="panel">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          {company.companyLogo && <img src={assetUrl(company.companyLogo)} alt={company.applicationName} className="max-h-20 max-w-56 object-contain" />}
          <div>
            <h1 className="text-2xl font-semibold">{company.applicationName}</h1>
          </div>
        </div>
      </section>
      <section>
        <div className="panel">
          <h2 className="mb-3 text-lg font-semibold">Entreprise</h2>
          <dl className="space-y-2 text-sm">
            <Row label="Email" value={company.email} />
            <Row label="Telephone" value={company.phone} />
            <Row label="Site" value={company.website} />
            <Row label="Adresse" value={[company.address, company.city].filter(Boolean).join(', ')} />
          </dl>
        </div>
      </section>
    </div>
  );
}

function Row({ label, value }: { label: string; value?: string }) {
  return <div className="flex justify-between gap-4 border-b border-slate-100 pb-2"><dt className="text-muted">{label}</dt><dd className="text-right font-medium">{value || '-'}</dd></div>;
}
