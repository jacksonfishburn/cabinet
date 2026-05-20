import { useState, useEffect } from "react";
import type { FileRecord } from "../types";
import { deleteItem, insert, peek } from "../api";


function testFiles() {
    return {
    "my-project": {
      name: "my-project",
      sizeBytes: 204800,
      md5: "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
      createdAt: "2025-05-01T12:00:00",
      updatedAt: "2025-05-01T14:30:00",
    },
    "config-backup": {
      name: "config-backup",
      sizeBytes: 512,
      md5: "deadbeefdeadbeefdeadbeefdeadbeef",
      createdAt: "2025-04-20T08:00:00",
      updatedAt: "2025-04-20T08:00:00",
    },
    "dotfiles": {
      name: "dotfiles",
      sizeBytes: 98304,
      md5: "cafebabecafebabecafebabecafebabe",
      createdAt: "2025-03-15T19:45:00",
      updatedAt: "2025-05-10T11:20:00",
    },
  };
}

export function useFiles() {
  const [files, setFiles] = useState<Record<string, FileRecord>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    try {
      setError(null);
      // const data = testFiles();
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

  const uploadFile = async (name: string, bytes: Blob) => {
    try {
      setError(null);
      await insert(name, bytes);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to upload '${name}'`);
    }
  };

  return { files, isLoading, error, refresh, deleteFile, uploadFile };
}