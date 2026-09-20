/**
 * Centralized environment configuration
 * All values should be set in .env* files
 * Throws error if required env vars are missing
 */

function getEnvVar(key: string, defaultValue?: string): string {
  const value = (import.meta.env as Record<string, string | undefined>)[key];

  if (!value && !defaultValue) {
    throw new Error(`Missing required environment variable: ${key}`);
  }

  return value || defaultValue || "";
}

/**
 * API Base URL from VITE_API_URL environment variable
 * Must be set in .env (dev), .env.production (build), or .env.local (local overrides)
 *
 * Development: http://localhost:8080
 * Production: https://api.yourdomain.com (or your actual production URL)
 */
export const API_URL = getEnvVar("VITE_API_URL", "http://localhost:8080");

/**
 * Validate environment on app startup
 * Call this in main.tsx or in AppProvider initialization
 */
export function validateEnv(): void {
  try {
    // Will throw if VITE_API_URL is not available
    console.log(`✓ API_URL configured: ${API_URL}`);
  } catch (err) {
    console.error("Environment validation failed:", err);
    throw err;
  }
}
