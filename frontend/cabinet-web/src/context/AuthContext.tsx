import { createContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthRequest, AuthResponse } from "../types";
import { clearToken, getToken, login, logout, register, setToken } from "../api";

type AuthUser = Pick<AuthResponse, "defaultCabinetId" | "username">;
const AUTH_SESSION_KEY = "cabinet.auth-session";

export interface AuthContextValue {
  token: string | null;
  user: AuthUser | null;
  defaultCabinetId: number | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  signIn: (request: AuthRequest) => Promise<AuthResponse>;
  signUp: (request: AuthRequest) => Promise<AuthResponse>;
  signOut: () => Promise<void>;
  setSession: (response: AuthResponse) => void;
  clearSession: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [defaultCabinetId, setDefaultCabinetId] = useState<number | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    const storedToken = getToken();
    const storedSession = localStorage.getItem(AUTH_SESSION_KEY);

    if (storedSession) {
      try {
        const parsedSession = JSON.parse(storedSession) as { token?: string; user?: Partial<AuthUser> & { id?: number } };

        if (parsedSession.token) {
          setTokenState(parsedSession.token);
        } else if (storedToken) {
          setTokenState(storedToken);
        }

        if (parsedSession.user) {
          const nextCabinetId = parsedSession.user.defaultCabinetId ?? parsedSession.user.id ?? null;
          if (nextCabinetId !== null) {
            setDefaultCabinetId(nextCabinetId);
            if (parsedSession.user.username) {
              setUser({
                defaultCabinetId: nextCabinetId,
                username: parsedSession.user.username,
              });
            }
          }
        }
      } catch {
        if (storedToken) {
          setTokenState(storedToken);
        }
      }
    } else if (storedToken) {
      setTokenState(storedToken);
    }

    setIsInitializing(false);
  }, []);

  const setSession = (response: AuthResponse) => {
    const nextUser = { defaultCabinetId: response.defaultCabinetId, username: response.username };
    setToken(response.token);
    setTokenState(response.token);
    setDefaultCabinetId(response.defaultCabinetId);
    setUser(nextUser);
    localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify({ token: response.token, user: nextUser }));
  };

  const clearSession = () => {
    clearToken();
    localStorage.removeItem(AUTH_SESSION_KEY);
    setTokenState(null);
    setUser(null);
    setDefaultCabinetId(null);
  };

  const signIn = async (request: AuthRequest): Promise<AuthResponse> => {
    const response = await login(request);
    setSession(response);
    return response;
  };

  const signUp = async (request: AuthRequest): Promise<AuthResponse> => {
    const response = await register(request);
    setSession(response);
    return response;
  };

  const signOut = async (): Promise<void> => {
    try {
      await logout();
    } finally {
      clearSession();
    }
  };

  const value = useMemo<AuthContextValue>(() => ({
    token,
    user,
    defaultCabinetId,
    isAuthenticated: Boolean(token),
    isInitializing,
    signIn,
    signUp,
    signOut,
    setSession,
    clearSession,
  }), [defaultCabinetId, isInitializing, token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext };