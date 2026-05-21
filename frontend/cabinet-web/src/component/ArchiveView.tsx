import type { ArchiveViewProps } from "../types"

const ArchiveView = ({ name, sizeBytes, createdAt, updatedAt, onDelete, onGrab, isDeleting = false, isGrabbing = false }: ArchiveViewProps) => {

    const handleArchiveDelete = async () => {
        await onDelete(name);
    };

    const handleArchiveGrab = async () => {
        if (!onGrab) return;
        await onGrab(name);
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const formatSize = (bytes: number) => {
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return `${size.toFixed(2)} ${units[unitIndex]}`;
    };

    return (
        <div className="w-full max-w-2xl mx-auto">
            {/* File Card Container */}
            <div className="bg-amber-50 border-2 border-amber-900 p-8 shadow-md">
                {/* File Name - Large at Top */}
                <div className="mb-8 pb-6 border-b-2 border-amber-800">
                    <h1 className="text-4xl font-bold text-amber-900 font-mono break-words">
                        {name}
                    </h1>
                </div>

                {/* File Details */}
                <div className="space-y-6 mb-8">
                    {/* Size */}
                    <div className="flex justify-between items-start">
                        <span className="text-xs font-bold uppercase tracking-wide text-amber-900">
                            Size
                        </span>
                        <span className="font-mono text-sm text-amber-800">
                            {formatSize(sizeBytes)}
                        </span>
                    </div>

                    {/* Created Date */}
                    <div className="flex justify-between items-start">
                        <span className="text-xs font-bold uppercase tracking-wide text-amber-900">
                            Created
                        </span>
                        <span className="font-mono text-sm text-amber-800">
                            {formatDate(createdAt)}
                        </span>
                    </div>

                    {/* Updated Date */}
                    <div className="flex justify-between items-start">
                        <span className="text-xs font-bold uppercase tracking-wide text-amber-900">
                            Updated
                        </span>
                        <span className="font-mono text-sm text-amber-800">
                            {formatDate(updatedAt)}
                        </span>
                    </div>
                </div>

                {/* Delete Button - Bottom Right */}
                <div className="pt-6 border-t-2 border-amber-800 flex justify-end gap-3">
                    <button
                        onClick={handleArchiveGrab}
                        disabled={!onGrab || isGrabbing}
                        className={
                            `px-4 py-2 text-amber-900 font-mono text-sm font-semibold uppercase tracking-wide transition-colors bg-amber-100 border-2 border-amber-800 hover:bg-amber-200` +
                            (isGrabbing ? ' opacity-60 cursor-not-allowed' : '')
                        }
                        aria-busy={isGrabbing}
                    >
                        {isGrabbing ? 'Grabbing...' : 'Grab'}
                    </button>

                    <button
                        onClick={handleArchiveDelete}
                        disabled={isDeleting}
                        className={
                            `px-4 py-2 text-amber-50 font-mono text-sm font-semibold uppercase tracking-wide transition-colors ` +
                            (isDeleting
                                ? 'bg-amber-700 opacity-60 cursor-not-allowed'
                                : 'bg-amber-900 hover:bg-amber-800')
                        }
                        aria-busy={isDeleting}
                    >
                        {isDeleting ? 'Deleting...' : 'Delete'}
                    </button>
                </div>
            </div>
        </div>
    )
}

export default ArchiveView
