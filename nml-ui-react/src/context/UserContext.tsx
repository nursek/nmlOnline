import React, { createContext, useState, useEffect, ReactNode } from 'react';

interface User {
  id: string;
  name: string;
  money: number;
  token: string;
}

interface UserContextType {
  user: User | null;
  setUser: (user: User | null) => void;
  refreshToken: () => Promise<boolean>;
}

export const UserContext = createContext<UserContextType | null>(null);

export const UserProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  const refreshToken = async () => {
    const res = await fetch("/api/auth/refresh", { method: "POST", credentials: "include" });
    if (res.ok) {
      const data = await res.json();
      setUser({
        id: data.id,
        name: data.name,
        money: data.money,
        token: data.token
      });
      return true;
    } else {
      setUser(null);
      return false;
    }
  };

  useEffect(() => {
    refreshToken();
    // eslint-disable-next-line
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser, refreshToken }}>
      {children}
    </UserContext.Provider>
  );
};
