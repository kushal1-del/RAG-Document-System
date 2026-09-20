import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { useAuth } from "../context/AuthContext";
import ThemeToggle from "../components/ThemeToggle";
import Input from "../components/Input";
import Button from "../components/Button";

function Login() {
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await login(email, password);
      loginUser(data.token);
      navigate("/dashboard");
    } catch (err) {
      setError("Login failed. Check your email and password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="min-h-screen bg-gradient-to-br from-white via-violet-50 to-fuchsia-50
      dark:from-zinc-950 dark:via-purple-950 dark:to-zinc-950
      flex items-center justify-center px-4 relative"
    >
      <div className="absolute top-6 right-6">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <p className="font-mono text-xs text-violet-600 dark:text-violet-400 tracking-widest uppercase mb-2">
            Document Intelligence
          </p>
          <h1
            className="font-display text-4xl font-semibold
            bg-gradient-to-r from-violet-600 to-fuchsia-500 bg-clip-text text-transparent"
          >
            Welcome back
          </h1>
        </div>

        <form
          onSubmit={handleSubmit}
          className="bg-white/80 dark:bg-zinc-900/80 backdrop-blur border
            border-violet-100 dark:border-violet-500/20 rounded-2xl p-7 shadow-xl shadow-violet-500/5"
        >
          {error && (
            <div className="mb-5 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20">
              <p className="text-red-600 dark:text-red-400 text-sm">{error}</p>
            </div>
          )}

          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            required
          />

          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />

          <div className="mt-6">
            <Button type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </Button>
          </div>

          <p className="text-zinc-500 dark:text-zinc-400 text-sm mt-5 text-center">
            No account?{" "}
            <a
              href="/register"
              className="text-violet-600 dark:text-violet-400 hover:underline font-medium"
            >
              Create one
            </a>
          </p>
        </form>
      </div>
    </div>
  );
}

export default Login;
