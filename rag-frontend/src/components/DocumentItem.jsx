function DocumentItem({ document, isSelected, onClick, onDelete }) {
  function handleDeleteClick(e) {
    e.stopPropagation();
    onDelete(document.id);
  }

  return (
    <div
      onClick={onClick}
      className={`group relative w-full text-left px-4 py-3 rounded-xl border transition-all cursor-pointer
        ${
          isSelected
            ? "bg-violet-50 dark:bg-violet-500/10 border-violet-400 dark:border-violet-500/50"
            : "bg-white dark:bg-zinc-800 border-zinc-200 dark:border-zinc-700 hover:border-violet-300 dark:hover:border-violet-500/30"
        }`}
    >
      <p className="text-zinc-900 dark:text-white text-sm font-medium truncate pr-6">
        {document.fileName}
      </p>
      <p className="font-mono text-xs text-zinc-400 dark:text-zinc-500 mt-1">
        {document.status}
      </p>

      <button
        onClick={handleDeleteClick}
        className="absolute top-3 right-3 w-6 h-6 rounded-md flex items-center justify-center
          text-zinc-300 dark:text-zinc-600 opacity-0 group-hover:opacity-100
          hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-500/10 dark:hover:text-red-400
          transition-all"
        aria-label="Delete document"
      >
        ✕
      </button>
    </div>
  );
}

export default DocumentItem;
