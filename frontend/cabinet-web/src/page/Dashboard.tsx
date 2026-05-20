import { useFiles } from "../hook/useFiles";
import { useState } from "react";
import type { FileRecord } from "../types";
import ArchiveList from "../component/ArchiveList";
import ArchiveView from "../component/ArchiveView";


function Dashboard() {
    const { files, error } = useFiles();
    const [selected, setSelected] = useState<FileRecord | null>(null);

    const archiveList = Object.values(files).map(f => ({ name: f.name, md5: f.md5 }));

    const findByMd5 = (md5: string) => {
        return Object.values(files).find(f => f.md5 === md5) ?? null;
    }

    return (
        <main className="min-h-screen bg-stone-600 text-slate-100 p-6 md:p-8
        bg-[linear-gradient(#BFB9A7,transparent_1px),linear-gradient(90deg,#BFB9A7,transparent_1px)] bg-[size:20px_20px]">
            <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 md:grid-cols-[420px_1fr]">
            <div className="pr-4 md:pr-6">
            <ArchiveList names={archiveList} onSelect={(md5) => setSelected(findByMd5(md5))} />
            </div>

            <div className="pl-0 md:pl-2 mt-25 ml-10">
                {error && (
                    <div className="mb-4 border-2 border-red-900 bg-red-950 px-4 py-3 text-sm text-red-100 font-mono">
                        {error}
                    </div>
                )}
                {selected && <ArchiveView {...selected} />}
            </div>
            </div>
        </main>
    )
}

export default Dashboard