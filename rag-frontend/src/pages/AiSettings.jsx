import { useEffect, useState } from "react";
import Button from "../components/Button";
import Navbar from "../components/Navbar";
import {
  addApiKey,
  listApiKeys,
  selectApiKey,
  disableApiKey,
} from "../api/aiKey";
import { getAiSettings, updateGenerationMode } from "../api/aiSettings";

function AiSettings() {
  const [settings, setSettings] = useState(null);
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingMode, setSavingMode] = useState(false);
  const [addingKey, setAddingKey] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [provider, setProvider] = useState("GEMINI");
  const [label, setLabel] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [showApiKey, setShowApiKey] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    setError("");

    const results = await Promise.allSettled([getAiSettings(), listApiKeys()]);
    const [settingsResult, keysResult] = results;

    if (settingsResult.status === "fulfilled") {
      setSettings(settingsResult.value);
    } else {
      const backendMessage =
        settingsResult.reason?.response?.data?.message ||
        settingsResult.reason?.response?.data?.error;
      setError(backendMessage || "Failed to load AI settings.");
    }

    if (keysResult.status === "fulfilled") {
      setApiKeys(Array.isArray(keysResult.value) ? keysResult.value : []);
    } else {
      const backendMessage =
        keysResult.reason?.response?.data?.message ||
        keysResult.reason?.response?.data?.error;
      setError((prev) =>
        prev ? prev : backendMessage || "Failed to load API keys.",
      );
    }

    setLoading(false);
  }

  async function handleModeChange(mode) {
    if (!settings || savingMode) return;
    if (settings.generationMode === mode) return;

    setSavingMode(true);
    setMessage("");
    setError("");

    try {
      const updatedSettings = await updateGenerationMode(mode);
      setSettings(updatedSettings);
      setMessage(
        mode === "FREE_TRIAL"
          ? "Free Trial mode is now active."
          : "Your API key mode is now active.",
      );
    } catch (err) {
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;
      setError(backendMessage || "Failed to change generation mode.");
    } finally {
      setSavingMode(false);
    }
  }

  async function handleAddApiKey(event) {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!label.trim()) {
      setError("Please enter a label for the API key.");
      return;
    }
    if (!apiKey.trim()) {
      setError("Please enter your Gemini API key.");
      return;
    }
    if (!expiresAt) {
      setError("Please select an expiry date.");
      return;
    }

    setAddingKey(true);

    try {
      await addApiKey(
        provider,
        label.trim(),
        apiKey.trim(),
        `${expiresAt}T23:59:59`,
      );
      setLabel("");
      setApiKey("");
      setExpiresAt("");
      setShowApiKey(false);
      await loadData();
      setMessage("API key added successfully.");
    } catch (err) {
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;
      setError(backendMessage || "Failed to add API key.");
    } finally {
      setAddingKey(false);
    }
  }

  async function handleSelectApiKey(id) {
    setMessage("");
    setError("");
    try {
      await selectApiKey(id);
      await loadData();
      setMessage("API key selected successfully.");
    } catch (err) {
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;
      setError(backendMessage || "Failed to select API key.");
    }
  }

  async function handleDisableApiKey(id) {
    const confirmed = window.confirm("Disable this API key?");
    if (!confirmed) return;

    setMessage("");
    setError("");
    try {
      await disableApiKey(id);
      await loadData();
      setMessage("API key disabled successfully.");
    } catch (err) {
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;
      setError(backendMessage || "Failed to disable API key.");
    }
  }

  const isFreeTrial = settings?.generationMode === "FREE_TRIAL";
  const isMyApiKey = settings?.generationMode === "MY_API_KEY";

  return (
    <div className="min-h-screen bg-gradient-to-br from-white via-violet-50/40 to-fuchsia-50/40 dark:from-zinc-950 dark:via-purple-950/40 dark:to-zinc-950">
      <Navbar />

      <div className="max-w-4xl mx-auto p-6">
        <h1 className="font-display text-2xl font-semibold text-zinc-900 dark:text-white mb-1">
          AI Settings
        </h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mb-6">
          Manage how your questions get answered.
        </p>

        {loading ? (
          <div className="py-20 text-center text-zinc-400">
            Loading AI settings...
          </div>
        ) : (
          <div className="space-y-6">
            {message && (
              <div className="px-4 py-3 rounded-xl bg-green-50 dark:bg-green-500/10 border border-green-200 dark:border-green-500/20 text-green-700 dark:text-green-400 text-sm">
                {message}
              </div>
            )}

            {error && (
              <div className="px-4 py-3 rounded-xl bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-red-700 dark:text-red-400 text-sm">
                {error}
              </div>
            )}

            <section className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
              <div className="mb-6">
                <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
                  Answer Generation
                </h2>
                <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                  Choose how your RAG questions are answered.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <button
                  type="button"
                  disabled={savingMode || !settings}
                  onClick={() => handleModeChange("FREE_TRIAL")}
                  className={`text-left p-5 rounded-2xl border-2 transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed ${
                    isFreeTrial
                      ? "border-violet-500 bg-violet-50 dark:bg-violet-500/10 shadow-md shadow-violet-500/10"
                      : "border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:border-violet-300 dark:hover:border-violet-700"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-10 h-10 rounded-xl flex items-center justify-center ${isFreeTrial ? "bg-violet-600 text-white" : "bg-zinc-100 dark:bg-zinc-700 text-zinc-500"}`}
                      >
                        AI
                      </div>
                      <h3 className="font-semibold text-zinc-900 dark:text-white">
                        Free Trial
                      </h3>
                    </div>
                    {isFreeTrial && (
                      <span className="text-xs font-semibold text-violet-600 dark:text-violet-400">
                        ACTIVE
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-4">
                    Use the application's Gemini API key.
                  </p>
                  <div className="mt-4">
                    <span className="text-sm font-mono text-zinc-700 dark:text-zinc-300">
                      {settings?.freeCallsUsed ?? 0} /{" "}
                      {settings?.freeCallsAllowed ?? 0} calls used
                    </span>
                  </div>
                </button>

                <button
                  type="button"
                  disabled={savingMode || !settings}
                  onClick={() => handleModeChange("MY_API_KEY")}
                  className={`text-left p-5 rounded-2xl border-2 transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed ${
                    isMyApiKey
                      ? "border-violet-500 bg-violet-50 dark:bg-violet-500/10 shadow-md shadow-violet-500/10"
                      : "border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 hover:border-violet-300 dark:hover:border-violet-700"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-10 h-10 rounded-xl flex items-center justify-center ${isMyApiKey ? "bg-violet-600 text-white" : "bg-zinc-100 dark:bg-zinc-700 text-zinc-500"}`}
                      >
                        KEY
                      </div>
                      <h3 className="font-semibold text-zinc-900 dark:text-white">
                        My API Key
                      </h3>
                    </div>
                    {isMyApiKey && (
                      <span className="text-xs font-semibold text-violet-600 dark:text-violet-400">
                        ACTIVE
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-4">
                    Use your own Gemini API key.
                  </p>
                  <div className="mt-4">
                    <span className="text-sm text-zinc-600 dark:text-zinc-300">
                      {settings?.selectedApiKeyId
                        ? "API key configured"
                        : "No API key selected"}
                    </span>
                  </div>
                </button>
              </div>

              {!settings && (
                <p className="text-xs text-red-500 mt-4">
                  Settings failed to load, so mode selection is disabled. Try
                  refreshing.
                </p>
              )}
            </section>

            <section className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
              <div className="mb-6">
                <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
                  My API Keys
                </h2>
                <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                  API keys are encrypted before being stored.
                </p>
              </div>

              {apiKeys.length === 0 ? (
                <div className="rounded-xl border border-dashed border-zinc-300 dark:border-zinc-700 py-10 text-center">
                  <p className="text-sm text-zinc-400">
                    No API keys added yet.
                  </p>
                  <p className="text-xs text-zinc-400 mt-1">
                    Add a Gemini API key below.
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {apiKeys.map((key) => (
                    <div
                      key={key.id}
                      className="p-4 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800"
                    >
                      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="font-medium text-zinc-900 dark:text-white">
                              {key.label}
                            </h3>
                            {settings?.selectedApiKeyId === key.id && (
                              <span className="text-xs px-2 py-1 rounded-full bg-violet-100 dark:bg-violet-500/20 text-violet-700 dark:text-violet-300">
                                Selected
                              </span>
                            )}
                            {!key.enabled && (
                              <span className="text-xs px-2 py-1 rounded-full bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-300">
                                Disabled
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-zinc-400 mt-2">
                            Provider: {key.provider}
                          </p>
                          <p className="text-xs text-zinc-400">
                            Expires: {key.expiresAt}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          {key.enabled &&
                            settings?.selectedApiKeyId !== key.id && (
                              <Button
                                variant="primary"
                                onClick={() => handleSelectApiKey(key.id)}
                              >
                                Select
                              </Button>
                            )}
                          {key.enabled && (
                            <Button
                              variant="secondary"
                              onClick={() => handleDisableApiKey(key.id)}
                            >
                              Disable
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
              <div className="mb-6">
                <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
                  Add Gemini API Key
                </h2>
                <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                  Configure your own Gemini API key.
                </p>
              </div>

              <form
                onSubmit={handleAddApiKey}
                autoComplete="off"
                className="space-y-5"
              >
                <input
                  type="text"
                  name="fakeusernameremembered"
                  autoComplete="username"
                  tabIndex={-1}
                  aria-hidden="true"
                  style={{
                    position: "absolute",
                    width: 0,
                    height: 0,
                    opacity: 0,
                    pointerEvents: "none",
                  }}
                />
                <input
                  type="password"
                  name="fakepasswordremembered"
                  autoComplete="new-password"
                  tabIndex={-1}
                  aria-hidden="true"
                  style={{
                    position: "absolute",
                    width: 0,
                    height: 0,
                    opacity: 0,
                    pointerEvents: "none",
                  }}
                />

                <div>
                  <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                    Provider
                  </label>
                  <select
                    value={provider}
                    onChange={(e) => setProvider(e.target.value)}
                    autoComplete="off"
                    className="w-full px-4 py-3 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10"
                  >
                    <option value="GEMINI">Google Gemini</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                    Label
                  </label>
                  <input
                    type="text"
                    name="ai-key-label-nofill"
                    value={label}
                    onChange={(e) => setLabel(e.target.value)}
                    placeholder="My Gemini Key"
                    autoComplete="off"
                    spellCheck="false"
                    className="w-full px-4 py-3 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white placeholder-zinc-400 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                    Gemini API Key
                  </label>
                  <div className="relative">
                    <input
                      type={showApiKey ? "text" : "password"}
                      name="gemini-api-key-nofill"
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      placeholder="Enter your Gemini API key"
                      autoComplete="off"
                      data-lpignore="true"
                      data-1p-ignore="true"
                      spellCheck="false"
                      className="w-full px-4 py-3 pr-12 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white placeholder-zinc-400 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowApiKey((v) => !v)}
                      tabIndex={-1}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-zinc-400 hover:text-violet-500"
                    >
                      {showApiKey ? "Hide" : "Show"}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                    Expiry Date
                  </label>
                  <input
                    type="date"
                    name="api-key-expiry"
                    value={expiresAt}
                    onChange={(e) => setExpiresAt(e.target.value)}
                    autoComplete="off"
                    min={new Date().toISOString().split("T")[0]}
                    className="w-full px-4 py-3 rounded-xl bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10"
                  />
                </div>

                <div className="pt-1">
                  <Button type="submit" variant="primary" disabled={addingKey}>
                    {addingKey ? "Adding..." : "Add API Key"}
                  </Button>
                </div>
              </form>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}

export default AiSettings;
