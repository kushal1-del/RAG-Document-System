import { useEffect, useState } from "react";
import { getAiSettings, updateGenerationMode } from "../api/aiSettings";

function AiSettings() {
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [changing, setChanging] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    loadSettings();
  }, []);

  async function loadSettings() {
    try {
      setLoading(true);
      setError("");

      const data = await getAiSettings();
      setSettings(data);
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Failed to load AI settings.";

      setError(message);
    } finally {
      setLoading(false);
    }
  }

  async function handleModeChange(mode) {
    if (!settings || changing) {
      return;
    }

    if (mode === settings.generationMode) {
      return;
    }

    try {
      setChanging(true);
      setError("");

      const updatedSettings = await updateGenerationMode(mode);

      setSettings(updatedSettings);
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Failed to change generation mode.";

      setError(message);
    } finally {
      setChanging(false);
    }
  }

  if (loading) {
    return (
      <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
        <p className="text-sm text-zinc-400">Loading AI settings...</p>
      </div>
    );
  }

  if (!settings) {
    return (
      <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-red-200 dark:border-red-800 rounded-2xl p-5">
        <p className="text-sm text-red-500">
          {error || "Unable to load AI settings."}
        </p>
      </div>
    );
  }

  const freeCallsUsed = settings.freeCallsUsed ?? 0;
  const freeCallsAllowed = settings.freeCallsAllowed ?? 0;

  const freeCallsRemaining = Math.max(freeCallsAllowed - freeCallsUsed, 0);

  const isFreeTrial = settings.generationMode === "FREE_TRIAL";

  const isMyApiKey = settings.generationMode === "MY_API_KEY";

  return (
    <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
            AI Generation
          </h2>

          <p className="text-xs text-zinc-400 mt-1">
            Choose how answers are generated.
          </p>
        </div>

        {changing && <span className="text-xs text-zinc-400">Updating...</span>}
      </div>

      {error && (
        <div className="mb-4 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20">
          <p className="text-xs text-red-500">{error}</p>
        </div>
      )}

      <div className="space-y-3">
        <button
          type="button"
          disabled={changing}
          onClick={() => handleModeChange("FREE_TRIAL")}
          className={`w-full text-left p-4 rounded-xl border transition-all ${
            isFreeTrial
              ? "border-violet-400 dark:border-violet-500/50 bg-violet-50 dark:bg-violet-500/10"
              : "border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:border-violet-300"
          }`}
        >
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-medium text-sm text-zinc-900 dark:text-white">
                  Free Trial
                </span>

                {isFreeTrial && (
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-violet-100 dark:bg-violet-500/20 text-violet-600 dark:text-violet-300">
                    Active
                  </span>
                )}
              </div>

              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                Use the application's Gemini API key.
              </p>
            </div>

            <div
              className={`w-4 h-4 rounded-full border-2 mt-1 ${
                isFreeTrial
                  ? "border-violet-600 bg-violet-600"
                  : "border-zinc-300 dark:border-zinc-600"
              }`}
            />
          </div>

          <div className="mt-3 flex items-center justify-between text-xs">
            <span className="text-zinc-500 dark:text-zinc-400">Calls used</span>

            <span className="font-mono text-zinc-700 dark:text-zinc-300">
              {freeCallsUsed} / {freeCallsAllowed}
            </span>
          </div>

          <div className="mt-2 h-1.5 rounded-full bg-zinc-200 dark:bg-zinc-700 overflow-hidden">
            <div
              className="h-full bg-violet-500 transition-all"
              style={{
                width:
                  freeCallsAllowed > 0
                    ? `${Math.min(
                        (freeCallsUsed / freeCallsAllowed) * 100,
                        100,
                      )}%`
                    : "0%",
              }}
            />
          </div>

          <p className="text-[11px] text-zinc-400 mt-2">
            {freeCallsRemaining} free call
            {freeCallsRemaining === 1 ? "" : "s"} remaining
          </p>
        </button>

        <button
          type="button"
          disabled={changing}
          onClick={() => handleModeChange("MY_API_KEY")}
          className={`w-full text-left p-4 rounded-xl border transition-all ${
            isMyApiKey
              ? "border-violet-400 dark:border-violet-500/50 bg-violet-50 dark:bg-violet-500/10"
              : "border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:border-violet-300"
          }`}
        >
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-medium text-sm text-zinc-900 dark:text-white">
                  My API Key
                </span>

                {isMyApiKey && (
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-violet-100 dark:bg-violet-500/20 text-violet-600 dark:text-violet-300">
                    Active
                  </span>
                )}
              </div>

              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                Use your own Gemini API key.
              </p>
            </div>

            <div
              className={`w-4 h-4 rounded-full border-2 mt-1 ${
                isMyApiKey
                  ? "border-violet-600 bg-violet-600"
                  : "border-zinc-300 dark:border-zinc-600"
              }`}
            />
          </div>

          <div className="mt-3 px-3 py-2 rounded-lg bg-zinc-50 dark:bg-zinc-800/70">
            <p className="text-[11px] text-zinc-400">
              {settings.selectedApiKeyId
                ? "API key selected"
                : "No API key selected"}
            </p>
          </div>
        </button>
      </div>
    </div>
  );
}

export default AiSettings;
