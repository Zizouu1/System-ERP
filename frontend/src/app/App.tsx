import AppRouter from "./router";
import { ErrorBoundary } from "./layout/ErrorBoundary";

export default function App() {
    return (
        <ErrorBoundary>
            <AppRouter />
        </ErrorBoundary>
    );
}
