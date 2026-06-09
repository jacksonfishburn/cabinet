export interface FileRecord {
  name: string;
  sizeBytes: number;
  md5: string;
  createdAt: string;
  updatedAt: string;
}

export interface InsertResponse {
  name: string;
  sizeBytes: number;
  md5: string;
}

export interface CabinetInfo {
  id: number;
  name: string;
}

export interface ListCabinetsResponse {
  cabinets: CabinetInfo[];
}

export type ArchiveViewProps = FileRecord & {
  onDelete: (name: string) => Promise<void>;
  onGrab?: (name: string) => Promise<void>;
  isDeleting?: boolean;
  isGrabbing?: boolean;
};

export interface ArchiveListProps {
  names: {name: string, md5: string}[]
  onSelect: (md5: string) => void
  onUpload: (files: File[]) => Promise<void>
  isUploading?: boolean
}

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  defaultCabinetId: number;
  username: string;
  token: string;
}