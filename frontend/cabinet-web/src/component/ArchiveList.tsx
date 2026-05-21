import { useRef, type ChangeEvent } from "react";
import type { ArchiveListProps } from "../types"


const ArchiveList = ({ names, onSelect, onUpload, isUploading = false }: ArchiveListProps) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = "";

    if (!file) {
      return;
    }

    await onUpload(file);
  };

  return (
    <div className="relative w-full max-w-3xl mx-auto mb-4 min-h-[calc(100vh-4rem)]">
      <div
        aria-hidden="true"
        className="absolute inset-0 -translate-x-4 translate-y-4 bg-mist-800"
      />

      <div className="relative flex min-h-[calc(100vh-4rem)] flex-col bg-mist-600 px-4 pb-4 pt-8 md:px-5 md:pb-5 md:pt-10 border-4 border-mist-800">
        <div className="mb-4 text-center md:mb-5">
          <h2 className="font-serif text-3xl tracking-[0.18em] text-mist-900 md:text-4xl">
            Cabinet
          </h2>
        </div>

        <div className="bg-amber-100 rounded-lg shadow-lg p-8 border-4 border-amber-900">
          <div className="mb-6 flex items-center justify-between gap-3 border-b-2 border-amber-800 pb-4">
            <h2 className="text-sm font-bold uppercase tracking-wide text-amber-900">
              Files
            </h2>

            <div>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading}
                className="border border-dashed border-amber-900 bg-amber-100 px-4 py-2 font-mono text-sm tracking-wide text-amber-900 transition-colors hover:bg-amber-200 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isUploading ? "Uploading..." : "Insert"}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                className="hidden"
                onChange={handleFileChange}
                disabled={isUploading}
                aria-hidden="true"
              />
            </div>
          </div>

          {/* Archives Stack */}
          <div className="space-y-3">
            {names.length === 0 ? (
              <p className="text-amber-700 text-sm italic">No archives stored</p>
            ) : (
              names.map(({ name, md5 }) => (
                <div
                  key={md5}
                  onClick={() => onSelect(md5)}
                  className={`
                  cursor-pointer
                  transition-all
                  relative
                  
                  /* Folder tab styling */
                  bg-gradient-to-b from-amber-50 to-amber-100
                  border-2 border-amber-800
                  rounded-t-lg
                  shadow-xl shadow-black/40
                  px-6 py-4
                  
                  /* Hover and selected states */
                  hover:from-amber-100 hover:to-amber-50
                  hover:shadow-2xl
                  
                  /* Folder tab top accent */
                  before:absolute before:top-0 before:left-0 before:right-0
                  before:h-1 before:bg-gradient-to-r before:from-amber-700 before:to-amber-600
                  before:rounded-t-md
                `}
                >
                  <div className="text-amber-900 font-mono text-sm font-semibold truncate">
                    {name}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ArchiveList