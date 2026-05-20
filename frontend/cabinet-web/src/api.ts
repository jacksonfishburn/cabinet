
import type { FileRecord } from "./types";

const SERVER_URL = "/api";
const TOKEN = import.meta.env.VITE_TOKEN

const getHeaders = (contentType?: string): HeadersInit => ({
  'Authorization': `Bearer ${TOKEN}`,
  ...(contentType && { 'Content-Type': contentType }),
});


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
  const response = await fetch(`${SERVER_URL}/${name}`, {
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
  const response = await fetch(`${SERVER_URL}/${name}`, {
    method: 'GET',
    headers: getHeaders(),
  });

  if (!response.ok) {
    throw new Error(`Error grabbing '${name}': ${response.status} ${response.statusText}`);
  }

  return response.blob();
};


export const deleteItem = async (name: string): Promise<void> => {
  const response = await fetch(`${SERVER_URL}/${name}`, {
    method: 'DELETE',
    headers: getHeaders(),
  });

  if (!response.ok && response.status !== 204) {
    throw new Error(`Error deleting '${name}': ${response.status} ${response.statusText}`);
  }
};