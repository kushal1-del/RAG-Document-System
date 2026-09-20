function Pagination({ currentPage, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between px-5 py-3 border-t border-zinc-200 dark:border-zinc-800">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="text-xs font-medium px-3 py-1.5 rounded-md border border-zinc-200 dark:border-zinc-700
          text-zinc-600 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed
          hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors"
      >
        ← Previous
      </button>

      <span className="text-xs text-zinc-400 font-mono">
        Page {currentPage + 1} of {totalPages}
      </span>

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        className="text-xs font-medium px-3 py-1.5 rounded-md border border-zinc-200 dark:border-zinc-700
          text-zinc-600 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed
          hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors"
      >
        Next →
      </button>
    </div>
  );
}

export default Pagination;
