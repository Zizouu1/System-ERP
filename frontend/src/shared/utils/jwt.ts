export type JwtRole = "ROLE_ADMIN" | "ROLE_LOGISTIC" | "ROLE_PSF" | "ROLE_PF";

export type JwtPayload = {
    sub?: string;
    role?: JwtRole;
    iat?: number;
    exp?: number;
    [k: string]: unknown;
};

function base64UrlDecode(input: string): string {
    let str = input.replace(/-/g, "+").replace(/_/g, "/");
    const pad = str.length % 4;
    if (pad) str += "=".repeat(4 - pad);
    return atob(str);
}

export function decodeJwtPayload(token: string): JwtPayload | null {
    try {
        const parts = token.split(".");
        if (parts.length < 2) return null;
        const json = base64UrlDecode(parts[1]);
        return JSON.parse(json) as JwtPayload;
    } catch {
        return null;
    }
}

export function isExpired(token: string): boolean {
    const payload = decodeJwtPayload(token);
    if (!payload?.exp) return false;
    const nowSec = Math.floor(Date.now() / 1000);
    return payload.exp <= nowSec;
}

/** Convert backend ROLE_XXX → lowercase sidebar role */
export function toSidebarRole(jwtRole: string): "admin" | "logistic" | "psf" | "pf" | null {
    if (!jwtRole) return null;
    const cleanRole = jwtRole.trim().toUpperCase();
    switch (cleanRole) {
        case "ROLE_ADMIN": return "admin";
        case "ROLE_LOGISTIC": return "logistic";
        case "ROLE_PSF": return "psf";
        case "ROLE_PF": return "pf";
        default: return null;
    }
}

/** Get the default landing route for a given user role */
export function getHomePath(role: string | null): string {
    switch (role) {
        case "admin": return "/dashboard";
        case "logistic": return "/incoming";
        case "psf": return "/departement-1/production";
        case "pf": return "/production";
        default: return "/login";
    }
}

