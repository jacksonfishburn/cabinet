
import type { ArchiveListProps } from "../types"

const ArchiveList = ({ names }: ArchiveListProps) => {

  const handleArchiveClick = (md5: string) => {
    void md5;
  };

  return (
    <div className="w-full max-w-3xl mx-auto">
      {/* Cabinet Container */}
      <div className="bg-amber-100 rounded-lg shadow-lg p-8 border-4 border-amber-900">
        {/* Cabinet Label */}
        <div className="mb-6 pb-4 border-b-2 border-amber-800">
          <h2 className="text-sm font-bold uppercase tracking-wide text-amber-900">
            Cabinet Contents
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
                onClick={() => handleArchiveClick(md5)}
                className={`
                  cursor-pointer
                  transition-all
                  relative
                  
                  /* Folder tab styling */
                  bg-gradient-to-b from-amber-50 to-amber-100
                  border-2 border-amber-800
                  rounded-t-lg
                  px-6 py-4
                  
                  /* Hover and selected states */
                  hover:from-amber-100 hover:to-amber-200
                  hover:shadow-md
                  
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
  );
}

export default ArchiveList