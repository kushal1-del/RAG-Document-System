import { createContext, useContext, useState } from "react";
import { getRoleFromToken } from "../utils/jwt";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem("token"));
  const role = token ? getRoleFromToken(token) : null;

  function loginUser(newToken) {
    localStorage.setItem("token", newToken);
    setToken(newToken);
  }

  function logoutUser() {
    localStorage.removeItem("token");
    setToken(null);
  }

  const value = {
    token,
    role,
    isAdmin: role === "ADMIN",
    isAuthenticated: !!token,
    loginUser,
    logoutUser,
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
