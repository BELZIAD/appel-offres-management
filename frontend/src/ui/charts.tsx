import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { MonthlyStats, OrganizationStats, ResultsStats } from '../api/types';

export function MonthlyTenderChart({ data }: { data: MonthlyStats[] }) {
  if (!data.length) return <EmptyChart />;
  return <div className="h-72"><ResponsiveContainer><BarChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="month" /><YAxis allowDecimals={false} /><Tooltip /><Legend /><Bar dataKey="total" name="Total" fill="#334155" /><Bar dataKey="won" name="Gagne" fill="#047857" /><Bar dataKey="lost" name="Perdu" fill="#b91c1c" /></BarChart></ResponsiveContainer></div>;
}

export function ResultDonutChart({ data }: { data: ResultsStats }) {
  const rows = [{ name: 'Gagne', value: data.won, color: '#047857' }, { name: 'Perdu', value: data.lost, color: '#b91c1c' }, { name: 'En attente', value: data.pending, color: '#64748b' }, { name: 'Annule', value: data.cancelled, color: '#7c3aed' }];
  if (rows.every(r => r.value === 0)) return <EmptyChart />;
  return <div className="h-72"><ResponsiveContainer><PieChart><Pie data={rows} dataKey="value" nameKey="name" innerRadius={60} outerRadius={95}>{rows.map(r => <Cell key={r.name} fill={r.color} />)}</Pie><Tooltip /><Legend /></PieChart></ResponsiveContainer></div>;
}

export function OrganizationChart({ data }: { data: OrganizationStats[] }) {
  const rows = data.slice(0, 10);
  if (!rows.length) return <EmptyChart />;
  return <div className="h-72"><ResponsiveContainer><BarChart data={rows}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="organization" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="total" name="AO" fill="#334155" /><Bar dataKey="won" name="Gagne" fill="#047857" /></BarChart></ResponsiveContainer></div>;
}

function EmptyChart() {
  return <div className="flex h-72 items-center justify-center text-sm text-muted">Aucune donnee disponible pour cette periode.</div>;
}
