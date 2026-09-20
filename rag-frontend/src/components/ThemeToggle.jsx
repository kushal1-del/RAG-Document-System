import { useTheme } from "../context/ThemeContext";

function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className="w-10 h-10 rounded-full flex items-center justify-center
        bg-white border border-violet-200 text-violet-600 shadow-sm
        dark:bg-zinc-800 dark:border-violet-500/30 dark:text-violet-300
        hover:scale-105 transition-transform"
      aria-label="Toggle dark mode"
    >
      {isDark ? "☀️" : "🌙"}
    </button>
  );
}

export default ThemeToggle;
