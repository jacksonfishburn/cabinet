
import type { ArchiveListProps } from "../types"


const ArchiveList = ({ names, onSelect }: ArchiveListProps) => {

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
          {/* Files Label */}
          <div className="mb-6 pb-4 border-b-2 border-amber-800">
            <h2 className="text-sm font-bold uppercase tracking-wide text-amber-900">
              Files
            </h2>
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