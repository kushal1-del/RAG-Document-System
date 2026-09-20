import { useState, useEffect } from "react";
import {
  listUsers,
  listAllDocuments,
  createUser,
  disableUser,
  enableUser,
  hardDeleteUser,
} from "../api/admin";
import Navbar from "../components/Navbar";
import Button from "../components/Button";
import Input from "../components/Input";
import Pagination from "../components/Pagination";

function Admin() {
  const [tab, setTab] = useState("users");

  const [users, setUsers] = useState([]);
  const [userBucket, setUserBucket] = useState("active"); // "active" | "disabled"
  const [userSearch, setUserSearch] = useState("");
  const [userPage, setUserPage] = useState(0);
  const [userTotalPages, setUserTotalPages] = useState(0);

  const [documents, setDocuments] = useState([]);
  const [docSearch, setDocSearch] = useState("");
  const [docPage, setDocPage] = useState(0);
  const [docTotalPages, setDocTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newRole, setNewRole] = useState("USER");
  const [createError, setCreateError] = useState("");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (tab === "users") {
      loadUsers();
    } else {
      loadDocuments();
    }
  }, [tab, userPage, docPage]);

  async function loadUsers() {
    setLoading(true);
    try {
      const data = await listUsers(userPage, 20);
      setUsers(data.content);
      setUserTotalPages(data.totalPages);
    } catch (err) {
      console.error("Failed to load users", err);
    } finally {
      setLoading(false);
    }
  }

  async function loadDocuments() {
    setLoading(true);
    try {
      const data = await listAllDocuments(docPage, 20);
      setDocuments(data.content);
      setDocTotalPages(data.totalPages);
    } catch (err) {
      console.error("Failed to load documents", err);
    } finally {
      setLoading(false);
    }
  }

  async function handleDisable(userId) {
    const confirmed = window.confirm(
      "Disable this user? They will no longer be able to log in.",
    );
    if (!confirmed) return;

    try {
      await disableUser(userId);
      await loadUsers();
    } catch (err) {
      alert("Failed to disable user.");
    }
  }

  async function handleEnable(userId) {
    try {
      await enableUser(userId);
      await loadUsers();
    } catch (err) {
      alert("Failed to enable user.");
    }
  }

  async function handleHardDelete(userId) {
    const confirmed = window.confirm(
      "Permanently delete this user and ALL their documents? This cannot be undone.",
    );
    if (!confirmed) return;

    try {
      await hardDeleteUser(userId);
      await loadUsers();
    } catch (err) {
      alert("Failed to delete user.");
    }
  }

  async function handleCreateUser(e) {
    e.preventDefault();
    setCreateError("");
    setCreating(true);

    try {
      await createUser(newEmail, newPassword, newRole);
      setNewEmail("");
      setNewPassword("");
      setNewRole("USER");
      setShowCreateForm(false);
      await loadUsers();
    } catch (err) {
      const message = err.response?.data?.message || "Failed to create user.";
      setCreateError(message);
    } finally {
      setCreating(false);
    }
  }

  const filteredUsers = users
    .filter((u) => (userBucket === "active" ? u.enabled : !u.enabled))
    .filter((u) => u.email.toLowerCase().includes(userSearch.toLowerCase()));

  const filteredDocuments = documents.filter((doc) =>
    doc.fileName.toLowerCase().includes(docSearch.toLowerCase()),
  );

  return (
    <div
      className="min-h-screen bg-gradient-to-br from-white via-violet-50/40 to-fuchsia-50/40
      dark:from-zinc-950 dark:via-purple-950/40 dark:to-zinc-950"
    >
      <Navbar />

      <div className="max-w-5xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="font-display text-2xl font-semibold text-zinc-900 dark:text-white">
            Admin Panel
          </h1>
          <div className="w-40">
            <Button
              variant="primary"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              {showCreateForm ? "Cancel" : "+ Create User"}
            </Button>
          </div>
        </div>

        {showCreateForm && (
          <form
            onSubmit={handleCreateUser}
            className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl p-6 mb-6"
          >
            <h2 className="font-display text-lg font-semibold text-zinc-900 dark:text-white mb-4">
              Create New User
            </h2>

            {createError && (
              <div className="mb-4 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20">
                <p className="text-red-600 dark:text-red-400 text-sm">
                  {createError}
                </p>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
              <Input
                label="Email"
                type="email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                placeholder="user@example.com"
                required
              />
              <Input
                label="Password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="At least 8 characters"
                required
              />
              <div className="mb-4">
                <label className="block text-zinc-500 dark:text-zinc-400 text-xs font-semibold uppercase tracking-wide mb-2">
                  Role
                </label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-zinc-800
                    border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white
                    outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10 transition-all"
                >
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
            </div>

            <div className="w-40 mt-2">
              <Button type="submit" disabled={creating}>
                {creating ? "Creating..." : "Create"}
              </Button>
            </div>
          </form>
        )}

        <div className="flex gap-2 mb-6">
          <button
            onClick={() => setTab("users")}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
              ${
                tab === "users"
                  ? "bg-gradient-to-r from-violet-600 to-fuchsia-500 text-white"
                  : "bg-white dark:bg-zinc-800 text-zinc-500 border border-zinc-200 dark:border-zinc-700"
              }`}
          >
            Users
          </button>
          <button
            onClick={() => setTab("documents")}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
              ${
                tab === "documents"
                  ? "bg-gradient-to-r from-violet-600 to-fuchsia-500 text-white"
                  : "bg-white dark:bg-zinc-800 text-zinc-500 border border-zinc-200 dark:border-zinc-700"
              }`}
          >
            Documents
          </button>
        </div>

        {tab === "users" && (
          <div className="flex items-center justify-between mb-4 gap-3">
            <div className="flex gap-2">
              <button
                onClick={() => setUserBucket("active")}
                className={`text-xs font-medium px-3 py-1.5 rounded-md transition-colors
                  ${
                    userBucket === "active"
                      ? "bg-emerald-50 dark:bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/20"
                      : "bg-white dark:bg-zinc-800 text-zinc-500 border border-zinc-200 dark:border-zinc-700"
                  }`}
              >
                Active
              </button>
              <button
                onClick={() => setUserBucket("disabled")}
                className={`text-xs font-medium px-3 py-1.5 rounded-md transition-colors
                  ${
                    userBucket === "disabled"
                      ? "bg-zinc-200 dark:bg-zinc-700 text-zinc-700 dark:text-zinc-200 border border-zinc-300 dark:border-zinc-600"
                      : "bg-white dark:bg-zinc-800 text-zinc-500 border border-zinc-200 dark:border-zinc-700"
                  }`}
              >
                Disabled
              </button>
            </div>

            <input
              type="text"
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
              placeholder="Search by email..."
              className="w-56 px-3.5 py-2 rounded-lg text-sm
                bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700
                text-zinc-900 dark:text-white placeholder:text-zinc-400
                outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10 transition-all"
            />
          </div>
        )}

        {tab === "documents" && (
          <div className="flex justify-end mb-4">
            <input
              type="text"
              value={docSearch}
              onChange={(e) => setDocSearch(e.target.value)}
              placeholder="Search by file name..."
              className="w-56 px-3.5 py-2 rounded-lg text-sm
                bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700
                text-zinc-900 dark:text-white placeholder:text-zinc-400
                outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10 transition-all"
            />
          </div>
        )}

        <div className="bg-white/60 dark:bg-zinc-900/60 backdrop-blur border border-zinc-200 dark:border-zinc-800 rounded-2xl overflow-hidden">
          {loading ? (
            <p className="text-zinc-400 text-sm text-center py-10">
              Loading...
            </p>
          ) : tab === "users" ? (
            <>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-zinc-200 dark:border-zinc-800 text-left">
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Email
                    </th>
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Role
                    </th>
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Status
                    </th>
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((user) => (
                    <tr
                      key={user.id}
                      className="border-b border-zinc-100 dark:border-zinc-800/50"
                    >
                      <td className="px-5 py-3 text-zinc-900 dark:text-white">
                        {user.email}
                      </td>
                      <td className="px-5 py-3">
                        <span className="font-mono text-xs px-2 py-1 rounded-md bg-violet-50 dark:bg-violet-500/10 text-violet-600 dark:text-violet-400">
                          {user.role}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <span
                          className={`font-mono text-xs px-2 py-1 rounded-md
                            ${
                              user.enabled
                                ? "bg-emerald-50 dark:bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                                : "bg-zinc-100 dark:bg-zinc-800 text-zinc-500"
                            }`}
                        >
                          {user.enabled ? "active" : "disabled"}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex gap-2">
                          {user.enabled ? (
                            <button
                              onClick={() => handleDisable(user.id)}
                              className="text-xs px-3 py-1.5 rounded-md border border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-800"
                            >
                              Disable
                            </button>
                          ) : (
                            <button
                              onClick={() => handleEnable(user.id)}
                              className="text-xs px-3 py-1.5 rounded-md border border-emerald-200 dark:border-emerald-500/20 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-500/10"
                            >
                              Enable
                            </button>
                          )}
                          <button
                            onClick={() => handleHardDelete(user.id)}
                            className="text-xs px-3 py-1.5 rounded-md border border-red-200 dark:border-red-500/20 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {filteredUsers.length === 0 && (
                    <tr>
                      <td
                        colSpan={4}
                        className="px-5 py-10 text-center text-zinc-400"
                      >
                        No {userBucket} users found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
              <Pagination
                currentPage={userPage}
                totalPages={userTotalPages}
                onPageChange={setUserPage}
              />
            </>
          ) : (
            <>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-zinc-200 dark:border-zinc-800 text-left">
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      File Name
                    </th>
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Status
                    </th>
                    <th className="px-5 py-3 text-zinc-500 font-medium">
                      Owner ID
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDocuments.map((doc) => (
                    <tr
                      key={doc.id}
                      className="border-b border-zinc-100 dark:border-zinc-800/50"
                    >
                      <td className="px-5 py-3 text-zinc-900 dark:text-white">
                        {doc.fileName}
                      </td>
                      <td className="px-5 py-3">
                        <span className="font-mono text-xs px-2 py-1 rounded-md bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300">
                          {doc.status}
                        </span>
                      </td>
                      <td className="px-5 py-3 font-mono text-xs text-zinc-400 truncate max-w-[200px]">
                        {doc.ownerId}
                      </td>
                    </tr>
                  ))}
                  {filteredDocuments.length === 0 && (
                    <tr>
                      <td
                        colSpan={3}
                        className="px-5 py-10 text-center text-zinc-400"
                      >
                        No documents found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
              <Pagination
                currentPage={docPage}
                totalPages={docTotalPages}
                onPageChange={setDocPage}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default Admin;
