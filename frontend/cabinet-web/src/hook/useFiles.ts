import { useState, useEffect } from "react";
import type { FileRecord } from "../types";
import { deleteItem, insert, peek } from "../api";
import { zipSingleFile } from "../util/zipSingleFile";


export function useFiles() {
  const [files, setFiles] = useState<Record<string, FileRecord>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    try {
      setError(null);
      const data = await peek();
      setFiles(data);
    } catch (err) {
      setFiles({});
      setError(err instanceof Error ? err.message : "Failed to load files");
    }
  };

  useEffect(() => {
    refresh().finally(() => setIsLoading(false));
  }, []);

  const deleteFile = async (name: string) => {
    try {
      setError(null);
      await deleteItem(name);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to delete '${name}'`);
    }
  };

  const uploadFile = async (file: File) => {
    try {
      setError(null);
      const bytes = new Uint8Array(await file.arrayBuffer());
      const zipBlob = zipSingleFile(file.name, bytes, new Date(file.lastModified));
      await insert(file.name, zipBlob);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to upload '${file.name}'`);
    }
  };

  return { files, isLoading, error, refresh, deleteFile, uploadFile };
}