import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, refreshSession, setAccessToken } from '../api/client';
import { Role, User } from '../api/types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<Role>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    refreshSession()
      .then((result) => {
        if (cancelled) return;
        setUser(result ? (result.user as User) : null);
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    async login(email, password) {
      const response = await api.post<{ token: string; user: User }>('/auth/login', { email, password });
      setAccessToken(response.data.token);
      setUser(response.data.user);
      return response.data.user.role;
    },
    async logout() {
      try {
        await api.post('/auth/logout');
      } finally {
        setAccessToken(null);
      }
      setUser(null);
      window.location.href = '/login';
    }
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('AuthContext manquant');
  }
  return value;
}
