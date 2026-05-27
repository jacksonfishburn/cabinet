
import Dashboard from "./page/Dashboard";
import LoginPage from "./page/Login";
import { useAuth } from "./hook/useAuth";


function App() {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <main className="min-h-screen bg-stone-600 px-6 py-8 text-slate-100 bg-[linear-gradient(#BFB9A7,transparent_1px),linear-gradient(90deg,#BFB9A7,transparent_1px)] bg-[size:20px_20px]">
        <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-7xl items-center justify-center">
          <div className="border-4 border-mist-800 bg-mist-600 px-8 py-6 font-mono text-sm uppercase tracking-[0.3em] text-mist-900">
            Loading...
          </div>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <main>
      <Dashboard />
    </main>
  )
}

export default App
