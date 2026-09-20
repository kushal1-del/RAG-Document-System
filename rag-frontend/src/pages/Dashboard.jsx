import { useState, useEffect, useRef } from "react";

import { askQuestion, searchChunks } from "../api/ask";

import Navbar from "../components/Navbar";
import DocumentItem from "../components/DocumentItem";
import Button from "../components/Button";
import AiSettings from "../components/AiSettings";

import {
  listDocuments,
  uploadDocument,
  deleteDocument,
} from "../api/documents";

function Dashboard() {
  const [documents, setDocuments] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedDocId, setSelectedDocId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [asking, setAsking] = useState(false);
  const [showSources, setShowSources] = useState(false);

  const fileInputRef = useRef(null);
  const chatEndRef = useRef(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages]);

  async function loadDocuments() {
    try {
      const data = await listDocuments();
      setDocuments([...data].reverse());
    } catch (err) {
      console.error("Failed to load documents", err);
    }
  }

  async function handleFileSelected(e) {
    const file = e.target.files[0];

    if (!file) {
      return;
    }

    setUploading(true);
    setUploadError("");

    try {
      await uploadDocument(file);
      await loadDocuments();
    } catch (err) {
      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;

      setUploadError(backendMessage || "Upload failed. Please try again.");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  async function handleAsk(e) {
    e.preventDefault();

    const trimmed = question.trim();

    if (!trimmed) {
      return;
    }

    setMessages((prev) => [
      ...prev,
      {
        role: "user",
        text: trimmed,
      },
    ]);

    setQuestion("");
    setAsking(true);

    try {
      const result = await askQuestion(trimmed, selectedDocId);

      let sources = null;

      if (showSources) {
        try {
          sources = await searchChunks(trimmed, selectedDocId);
        } catch (err) {
          sources = null;
        }
      }

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: result,
          sources,
        },
      ]);
    } catch (err) {
      const status = err?.response?.status;

      const backendMessage =
        err?.response?.data?.message || err?.response?.data?.error;

      let displayText =
        backendMessage || "Something went wrong. Please try again.";

      if (status === 429) {
        displayText =
          "Too many requests right now — please wait a moment and try again.";
      }

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: displayText,
          isError: true,
        },
      ]);
    } finally {
      setAsking(false);
    }
  }

  async function handleDeleteDocument(documentId) {
    const confirmed = window.confirm(
      "Delete this document? This cannot be undone.",
    );

    if (!confirmed) {
      return;
    }

    try {
      await deleteDocument(documentId);

      if (selectedDocId === documentId) {
        setSelectedDocId(null);
      }

      await loadDocuments();
    } catch (err) {
      alert("Failed to delete document. Please try again.");
    }
  }

  const filteredDocuments = documents.filter((doc) =>
    doc.fileName.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  return (
    <div
      className="min-h-screen bg-gradient-to-br from-white via-violet-50/40 to-fuchsia-50/40
dark:from-zinc-950 dark:via-purple-950/40 dark:to-zinc-950"
    >
      <Navbar />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 p-6 max-w-7xl mx-auto">
        <div className="md:col-span-1">
          <div>
            <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5">
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
                  Documents
                </h2>

                <span className="font-mono text-xs text-zinc-400">
                  {documents.length}
                </span>
              </div>

              <input
                type="file"
                accept=".pdf,.docx,.txt,.md"
                ref={fileInputRef}
                onChange={handleFileSelected}
                className="hidden"
              />

              <div className="mb-3">
                <Button
                  variant="primary"
                  onClick={() => fileInputRef.current.click()}
                  disabled={uploading}
                >
                  {uploading ? "Uploading..." : "+ Upload Document"}
                </Button>
              </div>

              {uploadError && (
                <p className="text-red-500 text-xs mb-3">{uploadError}</p>
              )}

              {documents.length > 0 && (
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search documents..."
                  className="w-full px-3.5 py-2.5 rounded-xl mb-4 text-sm
bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700
text-zinc-900 dark:text-white placeholder:text-zinc-400
outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10
transition-all"
                />
              )}

              <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                <button
                  onClick={() => setSelectedDocId(null)}
                  className={`w-full text-left px-4 py-3 rounded-xl border transition-all text-sm font-medium
${
  selectedDocId === null
    ? "bg-violet-50 dark:bg-violet-500/10 border-violet-400 dark:border-violet-500/50 text-violet-700 dark:text-violet-300"
    : "bg-white dark:bg-zinc-800 border-zinc-200 dark:border-zinc-700 text-zinc-500 hover:border-violet-300"
}`}
                >
                  All documents
                </button>

                {filteredDocuments.map((doc) => (
                  <DocumentItem
                    key={doc.id}
                    document={doc}
                    isSelected={selectedDocId === doc.id}
                    onClick={() => setSelectedDocId(doc.id)}
                    onDelete={handleDeleteDocument}
                  />
                ))}

                {documents.length === 0 && (
                  <p className="text-zinc-400 text-sm text-center py-6">
                    No documents yet. Upload one to get started.
                  </p>
                )}

                {documents.length > 0 && filteredDocuments.length === 0 && (
                  <p className="text-zinc-400 text-sm text-center py-6">
                    No documents match "{searchTerm}"
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="md:col-span-2">
          <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-6 flex flex-col h-[80vh]">
            <div className="flex items-center justify-between mb-1">
              <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white">
                Ask a question
              </h2>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <span className="text-xs text-zinc-500 dark:text-zinc-400 font-medium">
                  Show sources
                </span>

                <button
                  type="button"
                  onClick={() => setShowSources(!showSources)}
                  className={`relative w-9 h-5 rounded-full transition-colors
${
  showSources
    ? "bg-gradient-to-r from-violet-600 to-fuchsia-500"
    : "bg-zinc-300 dark:bg-zinc-700"
}`}
                >
                  <span
                    className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform
${showSources ? "translate-x-4" : "translate-x-0"}`}
                  />
                </button>
              </label>
            </div>

            <p className="text-zinc-400 text-sm mb-4">
              {selectedDocId
                ? "Searching within the selected document"
                : "Searching across all your documents"}
            </p>

            <div className="flex-1 overflow-y-auto space-y-4 mb-4 pr-1">
              {messages.length === 0 && (
                <div className="h-full flex items-center justify-center text-zinc-300 dark:text-zinc-700 text-sm">
                  Ask something about your documents to get started
                </div>
              )}

              {messages.map((msg, i) => (
                <div
                  key={i}
                  className={`flex flex-col ${
                    msg.role === "user" ? "items-end" : "items-start"
                  }`}
                >
                  <div
                    className={`max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap
${
  msg.role === "user"
    ? "bg-gradient-to-r from-violet-600 to-fuchsia-500 text-white rounded-br-sm"
    : msg.isError
      ? "bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-red-600 dark:text-red-400 rounded-bl-sm"
      : "bg-violet-50/60 dark:bg-violet-500/5 border border-violet-200 dark:border-violet-500/20 text-zinc-800 dark:text-zinc-200 rounded-bl-sm"
}`}
                  >
                    {msg.text}
                  </div>

                  {msg.sources && msg.sources.length > 0 && (
                    <details className="max-w-[80%] mt-2 w-full">
                      <summary className="cursor-pointer text-xs font-medium text-violet-600 dark:text-violet-400 hover:underline">
                        View {msg.sources.length} source
                        {msg.sources.length > 1 ? "s" : ""}
                      </summary>

                      <div className="mt-2 space-y-2">
                        {msg.sources.map((source, idx) => (
                          <div
                            key={idx}
                            className="px-3 py-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200 dark:border-zinc-700"
                          >
                            <div className="flex items-center justify-between mb-1">
                              <span className="font-mono text-[10px] text-zinc-400">
                                chunk #{source.chunkIndex}
                              </span>

                              <span className="font-mono text-[10px] text-violet-500 dark:text-violet-400">
                                score {source.score.toFixed(3)}
                              </span>
                            </div>

                            <p className="text-xs text-zinc-600 dark:text-zinc-300 line-clamp-3">
                              {source.content}
                            </p>
                          </div>
                        ))}
                      </div>
                    </details>
                  )}
                </div>
              ))}

              {asking && (
                <div className="flex justify-start">
                  <div className="px-4 py-3 rounded-2xl rounded-bl-sm bg-violet-50/60 dark:bg-violet-500/5 border border-violet-200 dark:border-violet-500/20">
                    <span className="flex gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-bounce [animation-delay:-0.3s]" />

                      <span className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-bounce [animation-delay:-0.15s]" />

                      <span className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-bounce" />
                    </span>
                  </div>
                </div>
              )}

              <div ref={chatEndRef} />
            </div>

            <form onSubmit={handleAsk} className="flex gap-3">
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask something about your documents..."
                className="flex-1 px-4 py-3 rounded-xl bg-white dark:bg-zinc-800
border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white
placeholder:text-zinc-400 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10
transition-all"
              />

              <div className="w-24">
                <Button type="submit" disabled={asking}>
                  {asking ? "..." : "Ask"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
