import { useState, useEffect } from "react";
import type { FileRecord } from "../types";
import { ApiError, deleteItem, grab, insert, peek } from "../api";
import JSZip from "jszip";
import { useAuth } from "./useAuth";


export function useFiles() {
  const { clearSession } = useAuth();
  const [files, setFiles] = useState<Record<string, FileRecord>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const handleAuthFailure = (err: unknown) => {
    if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
      clearSession();
      return true;
    }

    return false;
  };

  const refresh = async () => {
    try {
      setError(null);
      const data = await peek();
      setFiles(data);
    } catch (err) {
      setFiles({});
      handleAuthFailure(err);
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
      handleAuthFailure(err);
      setError(err instanceof Error ? err.message : `Failed to delete '${name}'`);
    }
  };

  const uploadFile = async (files: File[]) => {
    try {
      setError(null);
      const archive = new JSZip();

      const uploadRoot = files.length > 0
        ? files[0].webkitRelativePath.split("/").filter(Boolean)[0] ?? ""
        : "";

      for (const file of files) {
        const bytes = new Uint8Array(await file.arrayBuffer());
        const relativePath = file.webkitRelativePath || file.name;
        const archivePath = uploadRoot && relativePath.startsWith(`${uploadRoot}/`)
          ? relativePath.slice(uploadRoot.length + 1)
          : relativePath;

        archive.file(archivePath, bytes, {
          date: new Date(file.lastModified),
          binary: true,
        });
      }

      const zipBlob = await archive.generateAsync({
        type: "blob",
        compression: "STORE",
      });

      const archiveName = uploadRoot || files[0].name;
      await insert(archiveName, zipBlob);
      await refresh();
    } catch (err) {
      handleAuthFailure(err);
      setError(err instanceof Error ? err.message : "Failed to upload folder");
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
      handleAuthFailure(err);
      setError(err instanceof Error ? err.message : `Failed to grab '${name}'`);
      throw err;
    }
  };

  return { files, isLoading, error, refresh, deleteFile, uploadFile, grabFile };
}