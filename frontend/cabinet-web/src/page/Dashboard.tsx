import { useFiles } from "../hook/useFiles";
import { useState } from "react";
import type { FileRecord } from "../types";
import ArchiveList from "../component/archiveList";
import ArchiveView from "../component/ArchiveView";


function Dashboard() {
    const { files, isLoading, deleteFile, uploadFile } = useFiles();
    const [selected, setSelected] = useState<FileRecord | null>(null);

    const archiveList = Object.values(files).map(f => ({ name: f.name, md5: f.md5 }));

    const findByMd5 = (md5: string) => {
        return Object.values(files).find(f => f.md5 === md5) ?? null;
    }

    return (
        <main className="min-h-screen bg-stone-700 text-slate-100 p-6 md:p-8">
            <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 md:grid-cols-[420px_1fr]">
            <div className="pr-4 md:pr-6">
            <ArchiveList names={archiveList} onSelect={(md5) => setSelected(findByMd5(md5))} />
            </div>

            <div className="pl-0 md:pl-2 mt-25 ml-10">        
                {selected && <ArchiveView {...selected} />}
            </div>
            </div>
        </main>
    )
}

export default Dashboard