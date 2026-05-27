
import type { AuthRequest, AuthResponse, FileRecord } from "./types";

const SERVER_URL = "/api";
const TOKEN_STORAGE_KEY = "cabinet.token";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
};

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
};

export const clearToken = (): void => {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
};

const getHeaders = (contentType?: string): HeadersInit => {
  const token = getToken();

  return {
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...(contentType && { 'Content-Type': contentType }),
  };
};

const readError = async (response: Response, fallback: string): Promise<never> => {
  const rawBody = await response.text();
  let message = `${fallback}: ${response.status} ${response.statusText}`;

  if (rawBody) {
    try {
      const parsed = JSON.parse(rawBody) as { error?: string };
      if (parsed.error) {
        message = parsed.error;
      } else {
        message = rawBody;
      }
    } catch {
      message = rawBody;
    }
  }

  throw new ApiError(response.status, `ERROR ${response.status}: ${message}`);
};

const fetchJson = async <T>(url: string, init: RequestInit, fallback: string): Promise<T> => {
  const response = await fetch(url, init);

  if (!response.ok) {
    await readError(response, fallback);
  }

  return response.json() as Promise<T>;
};

const authRequest = async (path: string, request: AuthRequest): Promise<AuthResponse> => {
  return fetchJson<AuthResponse>(`${SERVER_URL}/auth${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  }, `Error ${path.slice(1)}`);
};

export const register = async (request: AuthRequest): Promise<AuthResponse> => {
  return authRequest('/register', request);
};

export const login = async (request: AuthRequest): Promise<AuthResponse> => {
  return authRequest('/login', request);
};

export const logout = async (): Promise<void> => {
  const token = getToken();
  const response = await fetch(`${SERVER_URL}/auth/logout`, {
    method: 'DELETE',
    headers: {
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
  });

  if (!response.ok && response.status !== 204) {
    await readError(response, 'Error logging out');
  }
};


export const peek = async (): Promise<Record<string, FileRecord>> => {
  const response = await fetch(`${SERVER_URL}/peek`, {
    method: 'GET',
    headers: getHeaders(),
  });

  if (!response.ok) {
    await readError(response, 'Error peeking');
  }

  return response.json();
};


export const insert = async (name: string, zipBlob: Blob): Promise<FileRecord> => {
  const response = await fetch(`${SERVER_URL}/${encodeURIComponent(name)}`, {
    method: 'POST',
    headers: getHeaders('application/octet-stream'),
    body: zipBlob,
  });

  if (!response.ok) {
    await readError(response, `Error inserting '${name}'`);
  }

  return response.json();
};


export const grab = async (name: string): Promise<Blob> => {
  const response = await fetch(`${SERVER_URL}/${encodeURIComponent(name)}`, {
    method: 'GET',
    headers: getHeaders(),
  });

  if (!response.ok) {
    await readError(response, `Error grabbing '${name}'`);
  }

  return response.blob();
};


export const deleteItem = async (name: string): Promise<void> => {
  const response = await fetch(`${SERVER_URL}/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    headers: getHeaders(),
  });

  if (!response.ok && response.status !== 204) {
    await readError(response, `Error deleting '${name}'`);
  }
};