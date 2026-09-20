export function decodeToken(token) {
  try {
    const payload = token.split(".")[1];
    const decoded = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(decoded);
  } catch (err) {
    return null;
  }
}

export function getRoleFromToken(token) {
  const decoded = decodeToken(token);
  return decoded?.role || null;
}
