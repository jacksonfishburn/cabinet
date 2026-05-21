export interface FileRecord {
  name: string;
  sizeBytes: number;
  md5: string;
  createdAt: string;
  updatedAt: string;
}

export type ArchiveViewProps = FileRecord & {
    onDelete: (name: string) => Promise<void>;
    isDeleting?: boolean;
};

export interface ArchiveListProps {
  names: {name: string, md5: string}[]
  onSelect: (md5: string) => void
}