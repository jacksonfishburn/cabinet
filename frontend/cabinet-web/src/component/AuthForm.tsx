type AuthMode = "login" | "register";

export interface AuthFormProps {
  mode: AuthMode;
  username: string;
  password: string;
  isSubmitting?: boolean;
  error?: string | null;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onToggleMode: () => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

const AuthForm = ({
  mode,
  username,
  password,
  isSubmitting = false,
  error,
  onUsernameChange,
  onPasswordChange,
  onToggleMode,
  onSubmit,
}: AuthFormProps) => {
  const isLogin = mode === "login";

  return (
    <div className="relative w-full max-w-2xl mx-auto">
      <div
        aria-hidden="true"
        className="absolute inset-0 translate-x-4 translate-y-4 bg-mist-800"
      />

      <section className="relative border-4 border-mist-800 bg-mist-600 px-5 py-6 md:px-8 md:py-8">
        <div className="mb-6 text-center md:mb-8">
          <h1 className="font-serif text-4xl tracking-[0.18em] text-mist-900 md:text-5xl">
            Cabinet
          </h1>
          <p className="mt-3 text-sm font-mono uppercase tracking-[0.3em] text-mist-900/80">
            {isLogin ? "Sign in to continue" : "Create your account"}
          </p>
        </div>

        <form onSubmit={onSubmit} className="bg-amber-100 border-4 border-amber-900 p-6 md:p-8 shadow-2xl shadow-black/30">
          <div className="mb-6 space-y-2">
            <label htmlFor="username" className="block text-xs font-bold uppercase tracking-[0.25em] text-amber-900">
              Username
            </label>
            <input
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(event) => onUsernameChange(event.currentTarget.value)}
              className="w-full border-2 border-amber-900 bg-amber-50 px-4 py-3 font-mono text-sm text-amber-950 outline-none transition focus:bg-white focus:ring-2 focus:ring-amber-800"
              placeholder="your-name"
              spellCheck="false"
            />
          </div>

          <div className="mb-3 space-y-2">
            <label htmlFor="password" className="block text-xs font-bold uppercase tracking-[0.25em] text-amber-900">
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete={isLogin ? "current-password" : "new-password"}
              value={password}
              onChange={(event) => onPasswordChange(event.currentTarget.value)}
              className="w-full border-2 border-amber-900 bg-amber-50 px-4 py-3 font-mono text-sm text-amber-950 outline-none transition focus:bg-white focus:ring-2 focus:ring-amber-800"
              placeholder="••••••••"
            />
          </div>

          {error && (
            <div className="mb-4 border-2 border-red-900 bg-red-950 px-4 py-3 text-sm font-mono text-red-100">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full border-2 border-amber-900 bg-amber-200 px-4 py-3 font-mono text-sm uppercase tracking-[0.25em] text-amber-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? (isLogin ? "Signing in..." : "Creating...") : (isLogin ? "Sign in" : "Register")}
          </button>

          <div className="mt-5 border-t-2 border-amber-800 pt-4 text-center">
            <button
              type="button"
              onClick={onToggleMode}
              className="font-mono text-xs uppercase tracking-[0.3em] text-amber-900 transition hover:text-amber-700"
            >
              {isLogin ? "Need an account? Register" : "Already have an account? Sign in"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
};

export default AuthForm;