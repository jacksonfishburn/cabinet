
import type { AuthRequest, AuthResponse, FileRecord } from "./types";

const SERVER_URL = "/api";
const TOKEN_STORAGE_KEY = "cabinet.token";

export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_STORAGE_KEY) ?? import.meta.env.VITE_TOKEN ?? null;
};

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
};

export const clearToken = (): void => {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
};

const getHeaders = (contentType?: string): HeadersInit => ({
  ...(getToken() && { 'Authorization': `Bearer ${getToken()}` }),
  ...(contentType && { 'Content-Type': contentType }),
});

const readError = async (response: Response, fallback: string): Promise<never> => {
  const message = await response.text();
  throw new Error(message || `${fallback}: ${response.status} ${response.statusText}`);
};

const fetchJson = async <T>(url: string, init: RequestInit, fallback: string): Promise<T> => {
  const response = await fetch(url, init);

  if (!response.ok) {
    await readError(response, fallback);
  }

  return response.json() as Promise<T>;
};

const loginWithBody = async (request: AuthRequest): Promise<AuthResponse> => {
  const payload = JSON.stringify(request);

  return new Promise<AuthResponse>((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.open('GET', `${SERVER_URL}/auth/login`);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.responseType = 'text';

    xhr.onload = () => {
      const text = xhr.responseText || '';

      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(text || `Error logging in: ${xhr.status} ${xhr.statusText}`));
        return;
      }

      try {
        resolve(JSON.parse(text) as AuthResponse);
      } catch {
        reject(new Error('Error logging in: invalid response'));
      }
    };

    xhr.onerror = () => {
      reject(new Error('Error logging in: network failure'));
    };

    xhr.send(payload);
  });
};

export const register = async (request: AuthRequest): Promise<AuthResponse> => {
  return fetchJson<AuthResponse>(`${SERVER_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  }, 'Error registering');
};

export const login = async (request: AuthRequest): Promise<AuthResponse> => {
  return loginWithBody(request);
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
    throw new Error(`Error peeking: ${response.status} ${response.statusText}`);
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
    throw new Error(`Error inserting '${name}': ${response.status} ${response.statusText}`);
  }

  return response.json();
};


export const grab = async (name: string): Promise<Blob> => {
  const response = await fetch(`${SERVER_URL}/${encodeURIComponent(name)}`, {
    method: 'GET',
    headers: getHeaders(),
  });

  if (!response.ok) {
    throw new Error(`Error grabbing '${name}': ${response.status} ${response.statusText}`);
  }

  return response.blob();
};


export const deleteItem = async (name: string): Promise<void> => {
  const response = await fetch(`${SERVER_URL}/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    headers: getHeaders(),
  });

  if (!response.ok && response.status !== 204) {
    throw new Error(`Error deleting '${name}': ${response.status} ${response.statusText}`);
  }
};