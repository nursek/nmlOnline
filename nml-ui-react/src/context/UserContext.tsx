import React, { createContext, useState, useEffect, ReactNode } from 'react';

interface User {
  id: string;
  name: string;
  token: string;
}

interface UserContextType {
  user: User | null;
  setUser: (user: User | null) => void;
  refreshToken: () => Promise<boolean>;
  logout: () => Promise<void>;
}

export const UserContext = createContext<UserContextType | null>(null);

export const UserProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  const refreshToken = async () => {
    try {
      const res = await fetch("/api/auth/refresh", { method: "POST", credentials: "include" });
      const data = await res.json();
      if (data.valid) {
        setUser({
          id: data.id,
          name: data.name,
          token: data.token
        });
        return true;
      } else {
        setUser(null);
        return false;
      }
    } catch {
      setUser(null);
      return false;
    }
  };

  const logout = async () => {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
    setUser(null);
  };

  useEffect(() => {
    refreshToken();
    // eslint-disable-next-line
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser, refreshToken, logout }}>
      {children}
    </UserContext.Provider>
  );
};