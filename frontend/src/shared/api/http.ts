/**
 * Auth callback is set by AuthProvider to handle centralized 401/403 responses
 * This allows the HTTP layer to trigger logout and redirect without tight coupling
 */
let onAuthFailure: (() => void) | null = null;

export function setAuthFailureHandler(handler: () => void): void {
  onAuthFailure = handler;
}

export class ApiError extends Error {
  status: number;
  body?: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

type ApiOptions = {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
};

/**
 * Get auth token from memory (managed by AuthProvider)
 * AuthProvider will set this via setAuthToken()
 */
let authToken: string | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

function authHeader(): Record<string, string> {
  if (!authToken) return {};
  return { Authorization: `Bearer ${authToken}` };
}

/**
 * Handle authentication failures (401, 403)
 * @param status HTTP status code
 */
function handleAuthFailure(status: number): void {
  if (status === 401) {
    // Trigger centralized logout in AuthProvider
    onAuthFailure?.();
  }
}

export async function apiJson<T>(
  url: string,
  opts: ApiOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {
    ...authHeader(),
    ...(opts.headers ?? {}),
  };

  let body: BodyInit | undefined = undefined;
  if (opts.body !== undefined) {
    headers["Content-Type"] = headers["Content-Type"] ?? "application/json";
    body = JSON.stringify(opts.body);
  }

  const res = await fetch(url, {
    method: opts.method ?? "GET",
    headers,
    body,
    signal: opts.signal,
  });

  const contentType = res.headers.get("content-type") || "";

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    let payload: unknown = undefined;

    try {
      if (contentType.includes("application/json")) {
        payload = await res.json();
        const payloadObj = payload as Record<string, unknown>;
        msg = (payloadObj?.message || payloadObj?.error || msg) as string;
      } else {
        const t = await res.text();
        if (t) msg = t;
      }
    } catch {
      // Could not parse error response, use default message
    }

    // Handle authentication failures
    handleAuthFailure(res.status);

    throw new ApiError(msg, res.status, payload);
  }

  if (contentType.includes("application/json")) {
    return (await res.json()) as T;
  }

  // Some endpoints may return empty response
  return undefined as unknown as T;
}

export async function apiBlob(
  url: string,
  opts: ApiOptions = {},
): Promise<Blob> {
  const headers: Record<string, string> = {
    ...authHeader(),
    ...(opts.headers ?? {}),
  };

  const res = await fetch(url, {
    method: opts.method ?? "GET",
    headers,
    body: opts.body as BodyInit,
    signal: opts.signal,
  });

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      msg = await res.text();
    } catch {
      // Could not read error response
    }

    // Handle authentication failures
    handleAuthFailure(res.status);

    throw new ApiError(msg, res.status);
  }

  return res.blob();
}
