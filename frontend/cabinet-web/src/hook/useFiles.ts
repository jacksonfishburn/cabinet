import { useState, useEffect } from "react";
import type { FileRecord } from "../types";
import { deleteItem, grab, insert, peek } from "../api";
import JSZip from "jszip";


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
      const archive = new JSZip();
      archive.file(file.name, bytes, {
        date: new Date(file.lastModified),
        binary: true,
      });

      const zipBlob = await archive.generateAsync({
        type: "blob",
        compression: "STORE",
      });

      await insert(file.name, zipBlob);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to upload '${file.name}'`);
    }
  };

  const grabFile = async (name: string): Promise<File> => {
    try {
      setError(null);
      const zipBlob = await grab(name);
      const archive = await JSZip.loadAsync(zipBlob);
      const entry = Object.values(archive.files).find((file) => !file.dir);

      if (!entry) {
        throw new Error("Archive is empty");
      }

      const bytes = await entry.async("uint8array");
      const fileBytes = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;

      return new File([fileBytes], entry.name || name, {
        type: "application/octet-stream",
        lastModified: entry.date?.getTime() ?? Date.now(),
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to grab '${name}'`);
      throw err;
    }
  };

  return { files, isLoading, error, refresh, deleteFile, uploadFile, grabFile };
}