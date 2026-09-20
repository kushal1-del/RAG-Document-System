import { useState } from "react";
import { useLocation, Link } from "react-router-dom";

import { useAuth } from "../context/AuthContext";

import ThemeToggle from "./ThemeToggle";
import Button from "./Button";

function Navbar() {
  const { logoutUser, isAdmin } = useAuth();

  const location = useLocation();

  const [isHelpOpen, setIsHelpOpen] = useState(false);

  const isOnAdminPage = location.pathname === "/admin";
  const isOnSettingsPage = location.pathname === "/ai-settings";

  return (
    <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-200 dark:border-zinc-800">
      <div>
        {isOnAdminPage && (
          <Link
            to="/dashboard"
            className="text-xs font-medium text-zinc-400 hover:text-violet-600 dark:hover:text-violet-400 mb-1 inline-block transition-colors"
          >
            Back to Dashboard
          </Link>
        )}

        {isOnSettingsPage && (
          <Link
            to="/dashboard"
            className="text-xs font-medium text-zinc-400 hover:text-violet-600 dark:hover:text-violet-400 mb-1 inline-block transition-colors"
          >
            Back to Dashboard
          </Link>
        )}

        <p className="font-mono text-xs text-violet-600 dark:text-violet-400 tracking-widest uppercase">
          Document Intelligence
        </p>

        <h1 className="font-display text-xl font-semibold text-zinc-900 dark:text-white">
          {isOnAdminPage
            ? "Admin Panel"
            : isOnSettingsPage
              ? "AI Settings"
              : "Dashboard"}
        </h1>
      </div>

      <div className="flex items-center gap-3">
        {isAdmin && !isOnAdminPage && !isOnSettingsPage && (
          <Link
            to="/admin"
            className="text-sm font-medium text-violet-600 dark:text-violet-400 hover:underline"
          >
            Admin Panel
          </Link>
        )}

        {!isOnAdminPage && !isOnSettingsPage && (
          <Link
            to="/ai-settings"
            className="text-sm font-medium text-violet-600 dark:text-violet-400 hover:underline"
          >
            AI Settings
          </Link>
        )}

        {/* API Key Help Dropdown */}
        <div className="relative">
          <button
            type="button"
            onClick={() => setIsHelpOpen((prev) => !prev)}
            className="flex items-center gap-1 text-sm font-medium text-violet-600 dark:text-violet-400 hover:text-violet-700 dark:hover:text-violet-300 transition-colors"
          >
            API Key Help
            <span className="text-xs">{isHelpOpen ? "▲" : "▼"}</span>
          </button>

          {isHelpOpen && (
            <div className="absolute right-0 mt-2 w-64 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 shadow-lg z-50 overflow-hidden">
              <a
                href="https://aistudio.google.com/apikey"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 px-4 py-3 text-sm text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
              >
                <span>🔑</span>
                <div>
                  <p className="font-medium">Create Gemini API Key</p>
                  <p className="text-xs text-zinc-400">Google AI Studio</p>
                </div>
              </a>

              <a
                href="https://ai.google.dev/gemini-api/docs/api-key"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 px-4 py-3 text-sm text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
              >
                <span>📖</span>
                <div>
                  <p className="font-medium">Gemini API Documentation</p>
                  <p className="text-xs text-zinc-400">
                    Official documentation
                  </p>
                </div>
              </a>

              <a
                href="https://www.youtube.com/watch?v=JdKcFCLotZY"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 px-4 py-3 text-sm text-zinc-700 dark:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
              >
                <span>▶️</span>
                <div>
                  <p className="font-medium">Watch Tutorial</p>
                  <p className="text-xs text-zinc-400">
                    How to create an API key
                  </p>
                </div>
              </a>
            </div>
          )}
        </div>

        <ThemeToggle />

        <div className="w-32">
          <Button variant="secondary" onClick={logoutUser}>
            Log Out
          </Button>
        </div>
      </div>
    </div>
  );
}

export default Navbar;
