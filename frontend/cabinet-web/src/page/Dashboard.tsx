import { useFiles } from "../hook/useFiles";
import { useState } from "react";
import type { FileRecord } from "../types";
import ArchiveList from "../component/ArchiveList";
import ArchiveView from "../component/ArchiveView";
import { useAuth } from "../hook/useAuth";


function Dashboard() {
    const { files, error, deleteFile, uploadFile, grabFile } = useFiles();
    const { signOut, user } = useAuth();
    const [selected, setSelected] = useState<FileRecord | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [isGrabbing, setIsGrabbing] = useState(false);

    const archiveList = Object.values(files).map(f => ({ name: f.name, md5: f.md5 }));

    const findByMd5 = (md5: string) => {
        return Object.values(files).find(f => f.md5 === md5) ?? null;
    }

    const handleDelete = async (name: string) => {
        setSelected(null);
        setIsDeleting(true);
        try {
            await deleteFile(name);
        } catch (e) {
        } finally {
            setIsDeleting(false);
        }
    };

    const handleGrab = async (name: string) => {
        setSelected(null);
        setIsGrabbing(true);
        try {
            const file = await grabFile(name);
            // trigger browser download
            const url = URL.createObjectURL(file);
            const a = document.createElement('a');
            a.href = url;
            a.download = file.name;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
        } catch (e) {
        } finally {
            setIsGrabbing(false);
        }
    };

    const handleUpload = async (files: File[]) => {
        setIsUploading(true);
        try {
            await uploadFile(files);
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <main className="relative min-h-screen bg-stone-600 text-slate-100 p-6 md:p-8
        bg-[linear-gradient(#BFB9A7,transparent_1px),linear-gradient(90deg,#BFB9A7,transparent_1px)] bg-[size:20px_20px]">
            <div className="fixed right-6 top-6 z-20 flex justify-end">
                <div className="flex w-full max-w-[240px] items-center justify-between gap-3 border-2 border-amber-900 bg-amber-100 px-4 py-2 text-amber-950 shadow-lg shadow-black/10">
                    <div className="min-w-0 truncate font-mono text-sm font-bold uppercase tracking-[0.22em]">
                        {user ? user.username : "Authenticated"}
                    </div>
                    <button
                        type="button"
                        onClick={signOut}
                        className="shrink-0 border border-amber-900 bg-amber-200 px-2 py-1 font-mono text-[10px] uppercase tracking-[0.18em] transition hover:bg-amber-300"
                    >
                        Logout
                    </button>
                </div>
            </div>

            <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 md:grid-cols-[420px_1fr]">
            <div className="pr-4 md:pr-6">
            <ArchiveList names={archiveList} onSelect={(md5) => setSelected(findByMd5(md5))} onUpload={handleUpload} isUploading={isUploading} />
            </div>

            <div className="pl-0 md:pl-2 mt-25 ml-10">
                {error && (
                    <div className="mb-4 border-2 border-red-900 bg-red-950 px-4 py-3 text-sm text-red-100 font-mono">
                        {error}
                    </div>
                )}
                {selected && (
                    <ArchiveView {...selected} onDelete={handleDelete} onGrab={handleGrab} isDeleting={isDeleting} isGrabbing={isGrabbing} />
                )}
            </div>
            </div>
        </main>
    )
}

export default Dashboard