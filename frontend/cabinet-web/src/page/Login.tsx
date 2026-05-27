import { useState } from "react";
import AuthForm from "../component/AuthForm";
import { useAuth } from "../hook/useAuth";
import type { AuthRequest } from "../types";

type AuthMode = "login" | "register";

const MIN_PASSWORD_LENGTH = 6;

const validateCredentials = ({ username, password }: AuthRequest) => {
  if (!username.trim()) {
    return "Username is required";
  }

  if (!password) {
    return "Password is required";
  }

  if (password.length < MIN_PASSWORD_LENGTH) {
    return `Password must be at least ${MIN_PASSWORD_LENGTH} characters`;
  }

  return null;
};

function LoginPage() {
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState<AuthMode>("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextRequest = {
      username: username.trim(),
      password,
    };

    const validationError = validateCredentials(nextRequest);
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      if (mode === "login") {
        await signIn(nextRequest);
      } else {
        await signUp(nextRequest);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${mode}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-stone-600 px-6 py-8 text-slate-100 bg-[linear-gradient(#BFB9A7,transparent_1px),linear-gradient(90deg,#BFB9A7,transparent_1px)] bg-[size:20px_20px]">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-7xl items-center justify-center">
        <div className="w-full">
          <AuthForm
            mode={mode}
            username={username}
            password={password}
            isSubmitting={isSubmitting}
            error={error}
            onUsernameChange={setUsername}
            onPasswordChange={setPassword}
            onToggleMode={() => {
              setMode((currentMode) => (currentMode === "login" ? "register" : "login"));
              setError(null);
            }}
            onSubmit={handleSubmit}
          />
        </div>
      </div>
    </main>
  );
}

export default LoginPage;